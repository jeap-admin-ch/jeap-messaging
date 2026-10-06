package ch.admin.bit.jeap.messaging.annotations.processor;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsolatedProcessorCompilationTest {
    @TempDir
    Path directory;

    @Test
    void isolatedProcessorReadsNestedApplicationClassAndAvroFromCompilerClasspath() throws Exception {
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(source("fixture/Outer.java", eventSource("fields", "registry"))), List.of("-proc:none"));
        // The application classes are deliberately absent from the processor path and from the test JVM.
        Path output = directory.resolve("application");
        compile(output, List.of(source("Consumer.java", consumerSource())), processorOptions(dependency));
        assertSchemas(output, "fields");

        // A second compilation in the same JVM must write its own resources, without stale deduplication state.
        Path second = directory.resolve("second");
        compile(second, List.of(source("Consumer.java", consumerSource())), processorOptions(dependency));
        assertSchemas(second, "fields");

        // Even if a processor dependency contains a different version, the application's version wins.
        Path shadow = directory.resolve("processor-dependency");
        compile(shadow, List.of(source("fixture/Outer.java", eventSource("wrong", "wrong-registry"))), List.of("-proc:none"));
        List<String> shadowOptions = new ArrayList<>(processorOptions(dependency));
        shadowOptions.set(3, shadow + File.pathSeparator + classpath());
        Path third = directory.resolve("shadowed");
        compile(third, List.of(source("Consumer.java", consumerSource())), shadowOptions);
        assertSchemas(third, "fields");
    }

    @Test
    void currentSourceWinsOverStaleCompiledClass() throws Exception {
        Path dependency = directory.resolve("dependency");
        Path event = source("fixture/Outer.java", eventSource("stale", "registry"));
        compile(dependency, List.of(event), List.of("-proc:none"));
        Files.writeString(event, eventSource("current", "registry"));
        Path output = directory.resolve("application");
        compile(output, List.of(event, source("Consumer.java", consumerSource())), processorOptions(dependency));
        assertSchemas(output, "current");
    }

    @Test
    void collidingSimpleNamesAndVersionsUseDistinctReferencedSchemas() throws Exception {
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(source("fixture/Outer.java", eventSource("first", "registry-one")),
                source("other/Outer.java", eventSource("second", "registry-two").replace("package fixture;", "package other;"))),
                List.of("-proc:none"));
        Path output = directory.resolve("application");
        compile(output, List.of(source("Consumer.java", consumerSource()),
                source("OtherConsumer.java", consumerSource().replace("fixture.Outer", "other.Outer")
                        .replace("appName=\"app\"", "appName=\"other\"").replace("class Consumer", "class OtherConsumer"))),
                processorOptions(dependency));
        try (var paths = Files.list(contracts(output))) {
            List<Path> files = paths.toList();
            assertThat(files.stream().filter(path -> path.toString().endsWith(".avsc"))).hasSize(2);
            for (Path path : files) {
                if (path.toString().endsWith(".json")) {
                    var contract = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    String expected = contract.get("appName").getAsString().equals("app") ? "first" : "second";
                    assertThat(Files.readString(contracts(output).resolve(contract.get("schemaResource").getAsString())))
                            .contains("\"name\":\"" + expected + "\"");
                }
            }
        }
    }

    @Test
    void unsupportedSourceInitializerExplainsWhySchemaIsUnavailable() throws Exception {
        Path output = directory.resolve("application");
        String source = eventSource("fields", "registry");
        int initializer = source.indexOf(" = new org.apache.avro.Schema.Parser()");
        source = source.substring(0, initializer) + " = null" + source.substring(source.indexOf(';', initializer));
        String diagnostics = compile(output, List.of(source("fixture/Outer.java", source), source("Consumer.java", consumerSource())),
                processorOptions(directory.resolve("absent")));
        assertThat(diagnostics).contains("unsupported SCHEMA$ initializer");
    }

    @Test
    void initializationFailureExplainsRootCauseAndLeavesContractWithoutSchema() throws Exception {
        Path dependency = directory.resolve("dependency");
        String broken = eventSource("fields", "registry").replace("return SCHEMA$;", "throw new IllegalStateException(\"broken schema factory\");");
        compile(dependency, List.of(source("fixture/Outer.java", broken)), List.of("-proc:none"));
        Path output = directory.resolve("application");
        String diagnostics = compile(output, List.of(source("Consumer.java", consumerSource())), processorOptions(dependency));
        assertThat(diagnostics).contains("broken schema factory", "No avro schema file generated");
        try (var paths = Files.list(contracts(output))) {
            for (Path contract : paths.toList()) {
                assertThat(JsonParser.parseString(Files.readString(contract)).getAsJsonObject().get("schemaResource").getAsString()).isEmpty();
            }
        }
    }

    @Test
    void assertionErrorInStaticInitializerFallsBack() throws Exception {
        initializerErrorFallsBack("AssertionError");
    }

    @Test
    void processorFailureStopsWorkerBeforeNextCompilation() throws Exception {
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(source("fixture/Outer.java", eventSource("fields", "registry"))), List.of("-proc:none"));
        Path consumer = source("Consumer.java", consumerSource());
        Path duplicate = source("Duplicate.java", consumerSource().replace("class Consumer", "class Duplicate"));
        assertThatThrownBy(() -> compile(directory.resolve("failed"), List.of(consumer, duplicate), processorOptions(dependency)))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("duplicate");
        assertNoWorkers();
        Path output = directory.resolve("next");
        compile(output, List.of(consumer), processorOptions(dependency));
        assertSchemas(output, "fields");
        assertNoWorkers();
    }

    @Test
    void bothProcessorsShareOneWorkerAndLoaderAndCloseIdempotently() throws Exception {
        String key = "jeap-schema-test-" + directory.getFileName();
        Path dependency = directory.resolve("dependency");
        String counted = eventSource("fields", "registry").replace("public static class Event {", """
                public static class Event {
                    static {
                        if (Thread.currentThread().getContextClassLoader() != ClassLoader.getPlatformClassLoader())
                            throw new AssertionError("worker inherited context loader");
                        System.setProperty("%s", Integer.toString(Integer.parseInt(System.getProperty("%s", "0")) + 1));
                    }
                """.formatted(key, key));
        compile(dependency, List.of(source("fixture/Outer.java", counted)), List.of("-proc:none"));
        var options = new ArrayList<>(processorOptions(dependency));
        options.set(5, JeapMessageAnnotationProcessor.class.getName() + "," + SharedSchemaTemplateProcessor.class.getName());
        try {
            Path output = directory.resolve("application");
            compile(output, List.of(source("Consumer.java", consumerSource())), options);
            assertSchemas(output, "fields");
            assertThat(System.getProperty(key)).isEqualTo("1");
            assertNoWorkers();
        } finally {
            System.clearProperty(key);
        }
    }

    public static class SharedSchemaTemplateProcessor extends JeapMessageTemplateAnnotationProcessor {
        @Override
        public java.util.Set<String> getSupportedAnnotationTypes() {
            return java.util.Set.of("*");
        }

        @Override
        public boolean process(java.util.Set<? extends javax.lang.model.element.TypeElement> annotations,
                               javax.annotation.processing.RoundEnvironment round) {
            if (!round.processingOver()) {
                var type = processingEnv.getElementUtils().getTypeElement("fixture.Outer.Event");
                assertThat(ch.admin.bit.jeap.messaging.annotations.processor.util.AvroSchemaExtractor
                        .extractSchemaJson(processingEnv, type)).isPresent();
            }
            return super.process(annotations, round);
        }
    }

    @Test
    void serviceConfigurationErrorInStaticInitializerFallsBack() throws Exception {
        initializerErrorFallsBack("java.util.ServiceConfigurationError");
    }

    private void initializerErrorFallsBack(String error) throws Exception {
        Path dependency = directory.resolve("dependency");
        String broken = eventSource("fields", "registry").replace("public static class Event {", """
                public static class Event {
                    static { if (System.nanoTime() != 0) throw new %s("broken static initializer"); }
                """.formatted(error));
        compile(dependency, List.of(source("fixture/Outer.java", broken)), List.of("-proc:none"));
        Path output = directory.resolve("application");
        String diagnostics = compile(output, List.of(source("Consumer.java", consumerSource())), processorOptions(dependency));
        assertThat(diagnostics).contains(error, "broken static initializer", "No avro schema file generated");
        assertNoSchemas(output);
        assertNoWorkers();
    }

    @Test
    void jpsProxyUnwrapsForTreesAndUsesCurrentSource() throws Exception {
        proxySourceCompilation(UnwrappableProcessor.class, true);
    }

    @Test
    void unsupportedProxyOmitsSchemaInsteadOfLoadingStaleBytecode() throws Exception {
        proxySourceCompilation(UnsupportedProxyProcessor.class, false);
    }

    private void proxySourceCompilation(Class<?> processor, boolean supported) throws Exception {
        Path dependency = directory.resolve("dependency");
        Path event = source("fixture/Outer.java", eventSource("stale", "registry"));
        compile(dependency, List.of(event), List.of("-proc:none"));
        Files.writeString(event, eventSource("current", "registry"));
        var options = new ArrayList<>(processorOptions(dependency));
        options.set(5, processor.getName());
        Path output = directory.resolve("application");
        String diagnostics = compile(output, List.of(event, source("Consumer.java", consumerSource())), options);
        if (supported) {
            assertSchemas(output, "current");
        } else {
            assertThat(diagnostics).contains("Source origin unavailable", "refusing potentially stale");
            assertNoSchemas(output);
        }
    }

    public static class UnsupportedProxyProcessor extends JeapMessageAnnotationProcessor {
        @Override
        public java.util.Set<String> getSupportedAnnotationTypes() {
            return java.util.Set.of("*");
        }

        @Override
        public synchronized void init(javax.annotation.processing.ProcessingEnvironment environment) {
            Class<?>[] interfaces = this instanceof UnwrappableProcessor
                    ? new Class<?>[]{javax.annotation.processing.ProcessingEnvironment.class, org.jetbrains.jps.javac.APIWrappers.Delegate.class}
                    : new Class<?>[]{javax.annotation.processing.ProcessingEnvironment.class};
            var proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, (object, method, args) -> {
                if (method.getName().equals("delegate")) {
                    return environment;
                }
                // Preserve reflexivity of the wrapper, including use as a weak map key.
                if (method.getName().equals("equals")) {
                    return object == args[0];
                }
                return method.invoke(environment, args);
            });
            super.init((javax.annotation.processing.ProcessingEnvironment) proxy);
        }
    }

    public static class UnwrappableProcessor extends UnsupportedProxyProcessor { }

    @Test
    void repeatedJarCompilationsHaveBoundedClassLoadingUnderConstrainedMetaspace() throws Exception {
        Path log = directory.resolve("metaspace.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-XX:MaxMetaspaceSize=320m", "-cp", classpath(), getClass().getName(), directory.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly().waitFor();
        }
        assertThat(finished).as("Fork timed out; %s", Files.readString(log)).isTrue();
        assertThat(process.exitValue()).as(Files.readString(log)).isZero();
        assertThat(Files.readString(log)).contains("five compilations of 40 jar message types passed");
        // Keep the fork's independent measurements in the surefire report as well.
        System.out.println(Files.readString(log));
    }

    public static void main(String[] args) throws Exception {
        var test = new IsolatedProcessorCompilationTest();
        test.directory = Path.of(args[0]);
        test.manyJarTypes();
    }

    private void manyJarTypes() throws Exception {
        var events = new ArrayList<Path>();
        var consumers = new ArrayList<Path>();
        for (int i = 0; i < 40; i++) {
            events.add(source("fixture/Outer" + i + ".java", eventSource("field" + i, "registry")
                    .replace("class Outer", "class Outer" + i)
                    .replace("MESSAGE_TYPE_NAME = \"Event\"", "MESSAGE_TYPE_NAME = \"Event" + i + "\"")));
            consumers.add(source("Consumer" + i + ".java", consumerSource()
                    .replace("fixture.Outer", "fixture.Outer" + i).replace("class Consumer", "class Consumer" + i)));
        }
        Path dependency = directory.resolve("dependency");
        compile(dependency, events, List.of("-proc:none"));
        Path jar = directory.resolve("messages.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar)); var paths = Files.walk(dependency)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                output.putNextEntry(new JarEntry(dependency.relativize(path).toString()));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
        var classes = ManagementFactory.getClassLoadingMXBean();
        for (int i = 0; i < 5; i++) {
            long before = classes.getTotalLoadedClassCount();
            Path output = directory.resolve("run" + i);
            compile(output, consumers, processorOptions(jar));
            long loaded = classes.getTotalLoadedClassCount() - before;
            System.out.println("compilation " + i + ": loaded " + loaded + " classes");
            // Deterministic bound: per-type loaders load ~25,000 classes. No System.gc() assumption.
            assertThat(loaded).isLessThan(5000);
            try (var paths = Files.list(contracts(output))) {
                assertThat(paths.filter(path -> path.toString().endsWith(".avsc")).count()).isEqualTo(40);
            }
            assertNoWorkers();
        }
        System.out.println("five compilations of 40 jar message types passed");
    }

    private static void assertNoWorkers() {
        assertThat(Thread.getAllStackTraces().keySet()).noneMatch(thread -> thread.getName().equals("jeap-schema-extraction"));
    }

    private void assertNoSchemas(Path output) throws Exception {
        try (var paths = Files.list(contracts(output))) {
            List<Path> contracts = paths.toList();
            assertThat(contracts).hasSize(2).allMatch(path -> path.toString().endsWith(".json"));
            for (Path contract : contracts) {
                assertThat(JsonParser.parseString(Files.readString(contract)).getAsJsonObject().get("schemaResource").getAsString()).isEmpty();
            }
        }
    }

    private List<String> processorOptions(Path dependency) {
        return List.of("-classpath", dependency + File.pathSeparator + classpath(),
                "-processorpath", classpath(), "-processor", JeapMessageAnnotationProcessor.class.getName());
    }

    private String compile(Path output, List<Path> sources, List<String> extraOptions) throws Exception {
        Files.createDirectories(output);
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<String> options = new ArrayList<>(List.of("-d", output.toString(), "-classpath", classpath()));
            options.addAll(extraOptions);
            boolean success = compiler.getTask(null, manager, diagnostics, options, null,
                    manager.getJavaFileObjectsFromPaths(sources)).call();
            assertThat(success).as(diagnostics.getDiagnostics().toString()).isTrue();
            return diagnostics.getDiagnostics().toString();
        }
    }

    private String classpath() {
        return System.getProperty("java.class.path");
    }

    private Path source(String name, String content) throws Exception {
        Path path = directory.resolve(name);
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

    private Path contracts(Path output) {
        return output.resolve("ch/admin/bit/jeap/messaging/contracts");
    }

    private void assertSchemas(Path output, String field) throws Exception {
        try (var paths = Files.list(contracts(output))) {
            List<Path> files = paths.toList();
            assertThat(files.stream().filter(path -> path.toString().endsWith(".avsc"))).hasSize(1);
            var references = new ArrayList<String>();
            for (Path path : files) {
                if (path.toString().endsWith(".json")) {
                    String resource = JsonParser.parseString(Files.readString(path)).getAsJsonObject().get("schemaResource").getAsString();
                    references.add(resource);
                    assertThat(Files.readString(contracts(output).resolve(resource))).contains("\"name\":\"" + field + "\"");
                }
            }
            assertThat(references).hasSize(2);
            assertThat(references.get(0)).isEqualTo(references.get(1));
        }
    }

    private String consumerSource() {
        return """
                import ch.admin.bit.jeap.messaging.annotations.*;
                @JeapMessageConsumerContract(value=fixture.Outer.Event.TypeRef.class, appName="app")
                @JeapMessageProducerContract(value=fixture.Outer.Event.TypeRef.class, appName="app")
                public class Consumer {}
                """;
    }

    private String eventSource(String field, String registry) {
        return """
                package fixture;
                public class Outer {
                    public static class Event {
                        public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse(
                            "{\\"type\\":\\"record\\",\\"name\\":\\"Event\\",\\"fields\\":[{\\"name\\":\\"%s\\",\\"type\\":\\"string\\"}]}");
                        public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
                        public static class TypeRef implements ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata {
                            public static final String MESSAGE_TYPE_NAME = "Event";
                            public static final String MESSAGE_TYPE_VERSION = "1.0";
                            public static final String SYSTEM_NAME = "system";
                            public static final String REGISTRY_URL = "%s";
                            public static final String REGISTRY_BRANCH = "master";
                            public static final String REGISTRY_COMMIT = "commit";
                            public static final String COMPATIBILITY_MODE = "BACKWARD";
                            public static final String DEFAULT_TOPIC = "topic";
                        }
                    }
                }
                """.formatted(field, registry);
    }
}
