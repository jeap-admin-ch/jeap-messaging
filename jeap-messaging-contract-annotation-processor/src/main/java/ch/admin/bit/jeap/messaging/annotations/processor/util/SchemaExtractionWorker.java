package ch.admin.bit.jeap.messaging.annotations.processor.util;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** No compiler objects are retained here or accessed by the worker. */
final class SchemaExtractionWorker implements Runnable {
    record Result(String schema, String diagnostic) { }
    private record Resource(String packageName, String fileName, CompletableFuture<byte[]> bytes) { }

    private final BlockingQueue<Resource> resources = new LinkedBlockingQueue<>();
    private CompilerClasspathLoader loader = new CompilerClasspathLoader(this::readClass);
    private Thread thread;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        thread = new Thread(null, task, "jeap-schema-extraction", 0, false);
        thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
        thread.setDaemon(true);
        return thread;
    });

    Result extract(Filer filer, String className, String metadataName, Map<String, String> constants) {
        return execute(filer, () -> reflect(className, metadataName, constants));
    }

    Result normalizeSource(String json) {
        return execute(null, () -> {
            try {
                return new Result(new org.apache.avro.Schema.Parser().parse(json).toString(), null);
            } catch (Throwable failure) {
                CompilerTrees.rethrowFatal(failure);
                return new Result(null, describe(failure));
            }
        });
    }

    private Result execute(Filer filer, Callable<Result> operation) {
        var result = executor.submit(operation);
        try {
            while (!result.isDone()) {
                Resource resource = resources.poll(10, TimeUnit.MILLISECONDS);
                if (resource != null) {
                    // javac's Filer/file manager is not thread safe. Only the calling compiler thread uses it.
                    try (var stream = filer.getResource(StandardLocation.CLASS_PATH,
                            resource.packageName(), resource.fileName()).openInputStream()) {
                        resource.bytes().complete(stream.readAllBytes());
                    } catch (Exception ex) {
                        resource.bytes().completeExceptionally(new IOException(ex.toString()));
                    }
                }
            }
            return result.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted extracting schema", ex);
        } catch (ExecutionException ex) {
            CompilerTrees.rethrowFatal(ex.getCause());
            throw new IllegalStateException("Unexpected worker failure: " + ex.getCause());
        }
    }

    private byte[] readClass(String packageName, String fileName) throws IOException {
        var bytes = new CompletableFuture<byte[]>();
        resources.add(new Resource(packageName, fileName, bytes));
        try {
            return bytes.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Schema worker stopped");
        } catch (ExecutionException ex) {
            throw new IOException(ex.getCause().toString());
        }
    }

    private Result reflect(String className, String metadataName, Map<String, String> constants) {
        try {
            if (metadataName != null) {
                Class<?> metadata = Class.forName(metadataName, true, loader);
                for (var constant : constants.entrySet()) {
                    if (!constant.getValue().equals(metadata.getField(constant.getKey()).get(null))) {
                        throw new IllegalStateException("Compiled TypeRef metadata differs from compiler model: " + constant.getKey());
                    }
                }
            }
            Class<?> message = Class.forName(className, true, loader);
            Object schema;
            try {
                schema = message.getMethod("getClassSchema").invoke(null);
            } catch (NoSuchMethodException ex) {
                schema = message.getDeclaredField("SCHEMA$").get(null);
            }
            return new Result(schema.toString(), null);
        } catch (Throwable failure) { // Only strings cross the boundary, except fatal VM errors.
            CompilerTrees.rethrowFatal(failure);
            return new Result(null, describe(failure));
        }
    }

    private static String describe(Throwable failure) {
        StringBuilder text = new StringBuilder(failure.toString());
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        seen.add(failure);
        while (failure.getCause() != null && seen.add(failure.getCause())) {
            failure = failure.getCause();
            text.append("; caused by: ").append(failure);
        }
        return text.toString();
    }

    /** Also usable as a Cleaner action: contains no environment, Filer or element references. */
    @Override
    public synchronized void run() {
        executor.shutdownNow();
        boolean interrupted = false;
        if (thread != null) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        }
        if (loader != null) {
            loader.close();
            loader = null;
        }
        resources.clear();
        thread = null;
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
