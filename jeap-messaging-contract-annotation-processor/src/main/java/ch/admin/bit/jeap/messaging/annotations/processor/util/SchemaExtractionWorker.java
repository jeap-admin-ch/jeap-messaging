package ch.admin.bit.jeap.messaging.annotations.processor.util;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.time.Duration;
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
    record Result(String schema, String diagnostic, boolean timedOut) {
        Result(String schema, String diagnostic) {
            this(schema, diagnostic, false);
        }
    }
    private record Resource(String packageName, String fileName, CompletableFuture<byte[]> bytes) { }

    private final BlockingQueue<Resource> resources = new LinkedBlockingQueue<>();
    private volatile CompilerClasspathLoader loader = new CompilerClasspathLoader(this::readClass);
    private volatile Thread thread;
    private volatile boolean stopped;
    private boolean timedOut;
    private final Duration extractionTimeout;
    private final Duration shutdownTimeout;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        thread = new Thread(null, task, "jeap-schema-extraction", 0, false);
        thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
        thread.setDaemon(true);
        return thread;
    });

    SchemaExtractionWorker() {
        this(Duration.ofSeconds(30), Duration.ofSeconds(1));
    }

    SchemaExtractionWorker(Duration extractionTimeout, Duration shutdownTimeout) {
        this.extractionTimeout = extractionTimeout;
        this.shutdownTimeout = shutdownTimeout;
    }

    boolean timedOut() {
        return timedOut;
    }

    Result extract(Filer filer, String className, String metadataName, Map<String, String> constants) {
        return execute(filer, () -> reflect(className, metadataName, constants));
    }

    // Package-private to exercise cancellation without initializing application classes in unit tests.
    Result execute(Filer filer, Callable<Result> operation) {
        if (stopped) {
            return new Result(null, "Schema extraction worker stopped; skipping further binary schemas in this compilation");
        }
        var result = executor.submit(operation);
        long started = System.nanoTime();
        try {
            while (!result.isDone()) {
                if (System.nanoTime() - started >= extractionTimeout.toNanos()) {
                    timedOut = true;
                    result.cancel(true);
                    run();
                    return new Result(null, "Schema extraction timed out after " + extractionTimeout +
                            "; worker stopped", true);
                }
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
            result.cancel(true);
            Thread.currentThread().interrupt();
            run();
            return new Result(null, "Interrupted extracting schema; worker stopped");
        } catch (ExecutionException ex) {
            CompilerTrees.rethrowFatal(ex.getCause());
            throw new IllegalStateException("Unexpected worker failure", ex.getCause());
        }
    }

    private byte[] readClass(String packageName, String fileName) throws IOException {
        var bytes = new CompletableFuture<byte[]>();
        Resource resource = new Resource(packageName, fileName, bytes);
        resources.add(resource);
        if (stopped) {
            resources.remove(resource);
            throw new IOException("Schema worker stopped");
        }
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
        CompilerClasspathLoader extractionLoader = loader;
        try {
            if (metadataName != null) {
                Class<?> metadata = Class.forName(metadataName, true, extractionLoader);
                for (var constant : constants.entrySet()) {
                    if (!constant.getValue().equals(metadata.getField(constant.getKey()).get(null))) {
                        throw new IllegalStateException("Compiled TypeRef metadata differs from compiler model: " + constant.getKey());
                    }
                }
            }
            Class<?> message = Class.forName(className, true, extractionLoader);
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
        stopped = true;
        executor.shutdownNow();
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(shutdownTimeout);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (loader != null) {
            loader.close();
            loader = null;
        }
        Resource resource;
        while ((resource = resources.poll()) != null) {
            resource.bytes().completeExceptionally(new IOException("Schema worker stopped"));
        }
        thread = null;
    }
}
