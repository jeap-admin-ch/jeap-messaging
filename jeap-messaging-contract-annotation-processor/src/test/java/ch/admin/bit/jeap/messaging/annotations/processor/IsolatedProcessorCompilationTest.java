package ch.admin.bit.jeap.messaging.annotations.processor;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsolatedProcessorCompilationTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"var", "lambda", "enclosing"})
    void sourceExtractionDoesNotAttributeTypesGeneratedByAnotherProcessor(String variant) throws Exception {
        String body = variant.equals("lambda")
                ? "var helper = new gen.Helper(); return java.util.List.of(helper).stream().map(value -> value.name()).findFirst().orElse(\"\");"
                : "var helper = new gen.Helper(); return helper.name();";
        String method = "public String useGeneratedType() { " + body + " }";
        String event = eventSource("fields", "registry");
        event = variant.equals("enclosing") ? event.replace("public class Outer {", "public class Outer { " + method)
                : event.replace("public static class TypeRef", method + " public static class TypeRef");
        var sources = List.of(source("fixture/Outer.java", event), source("Consumer.java", consumerSource()));
        var options = new ArrayList<>(processorOptions(directory.resolve("absent")));
        setOption(options, "-processor", HelperGeneratingProcessor.class.getName());
        compile(directory.resolve("control"), sources, options);
        setOption(options, "-processor", HelperGeneratingProcessor.class.getName() + "," + JeapMessageAnnotationProcessor.class.getName());
        Path output = directory.resolve("with-contracts");
        compile(output, sources, options);
        assertSchemas(output, "fields");
        assertThat(output.resolve("gen/Helper.class")).exists();
    }

    public static class HelperGeneratingProcessor extends AbstractProcessor {
        private boolean generated;

        @Override
        public Set<String> getSupportedAnnotationTypes() { return Set.of("*"); }

        @Override
        public SourceVersion getSupportedSourceVersion() { return SourceVersion.latest(); }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
            if (!generated && !round.processingOver()) {
                generated = true;
                try (var writer = processingEnv.getFiler().createSourceFile("gen.Helper").openWriter()) {
                    writer.write("package gen; public class Helper { public String name() { return \"helper\"; } }");
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
            return false;
        }
    }

    @Test
    void explicitBinaryRootIsNotMistakenForSource() throws Exception {
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(source("fixture/Outer.java", eventSource("fields", "registry"))), List.of("-proc:none"));
        Path output = directory.resolve("explicit-root");
        compile(ToolProvider.getSystemJavaCompiler(), output, List.of(source("Consumer.java", consumerSource())),
                processorOptions(dependency), List.of("fixture.Outer"));
        assertSchemas(output, "fields");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void eclipseWithoutJavacModuleSupportsBinaryExtractionAndSourceFallback(boolean generationEnabled) throws Exception {
        Path event = source("fixture/Outer.java", eventSource("fields", "registry"));
        Path consumer = source("Consumer.java", consumerSource());
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(event), List.of("-proc:none"));
        for (boolean binary : List.of(true, false)) {
            Path output = directory.resolve(binary ? "ecj-binary" : "ecj-source");
            Files.createDirectories(output);
            var command = new ArrayList<>(List.of(javaExecutable("java"), "--limit-modules", "java.se,jdk.unsupported,jdk.zipfs",
                    "-cp", classpath(), "org.eclipse.jdt.internal.compiler.batch.Main", "-24",
                    "-classpath", (binary ? dependency + File.pathSeparator : "") + classpath(),
                    "-processor", JeapMessageAnnotationProcessor.class.getName() + "," + LifecycleTemplateProcessor.class.getName(),
                    "-AmessageContractSchemaGenerationEnabled=" + generationEnabled, "-d", output.toString(), consumer.toString()));
            if (!binary) command.add(event.toString());
            String diagnostics = runCompilerProcess(binary ? "ecj-binary" : "ecj-source", command);
            if (binary && generationEnabled) assertSchemas(output, "fields");
            else assertNoSchemas(output);
            if (!generationEnabled) assertThat(diagnostics).doesNotContain("No avro schema", "NoClassDefFoundError");
        }
    }

    /** Exercise template round/cleanup hooks without relying on its separate template discovery mechanism. */
    public static class LifecycleTemplateProcessor extends JeapMessageTemplateAnnotationProcessor {
        @Override
        public Set<String> getSupportedAnnotationTypes() { return Set.of("*"); }

        @Override
        public Set<String> getSupportedOptions() { return Set.of(SchemaWriter.GENERATION_ENABLED); }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void olderGsonOnProcessorPathOmitsUnvalidatedSchemaWithoutFailingCompilation(boolean binary) throws Exception {
        Path oldGson = Path.of("target/test-fixtures/gson-2.8.9.jar").toAbsolutePath();
        assertThat(oldGson).exists();
        String processorPath = Arrays.stream(classpath().split(Pattern.quote(File.pathSeparator)))
                .map(entry -> Path.of(entry).getFileName().toString().startsWith("gson-") ? oldGson.toString() : entry)
                .collect(java.util.stream.Collectors.joining(File.pathSeparator));
        Path event = source("fixture/Outer.java", eventSource("fields", "registry"));
        Path consumer = source("Consumer.java", consumerSource());
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(event), List.of("-proc:none"));
        Path output = directory.resolve("old-gson");
        Files.createDirectories(output);
        var command = new ArrayList<>(List.of(javaExecutable("javac"), "-Werror", "-classpath",
                (binary ? dependency + File.pathSeparator : "") + classpath(), "-processorpath", processorPath,
                "-processor", JeapMessageAnnotationProcessor.class.getName(), "-d", output.toString(), consumer.toString()));
        if (!binary) command.add(event.toString());
        assertThat(runCompilerProcess("old-gson", command)).contains("Schema JSON validation unavailable", "Gson", "Strictness");
        assertNoSchemas(output);
    }

    private static String javaExecutable(String executable) {
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private String runCompilerProcess(String name, List<String> command) throws Exception {
        Path log = directory.resolve(name + ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly().waitFor();
        String diagnostics = Files.readString(log);
        assertThat(finished).as(diagnostics).isTrue();
        assertThat(process.exitValue()).as(diagnostics).isZero();
        return diagnostics;
    }

    @Test
    void readsMessageFromNamedModule() throws Exception {
        Path dependency = directory.resolve("module-dependency");
        compile(dependency, List.of(source("module-info.java", "module fixture.messages { exports fixture; }"),
                source("fixture/Outer.java", eventSource("fields", "registry"))),
                List.of("-proc:none", "--add-reads", "fixture.messages=ALL-UNNAMED"));
        Path jar = jar(dependency);
        Path output = directory.resolve("module-application");
        var options = new ArrayList<>(processorOptions(directory.resolve("absent")));
        options.addAll(List.of("--module-path", jar.toString(), "--add-reads", "fixture.consumer=ALL-UNNAMED",
                "--add-reads", "fixture.messages=ALL-UNNAMED"));
        compile(output, List.of(source("module-info.java", "module fixture.consumer { requires fixture.messages; }"),
                source("app/Consumer.java", "package app;\n" + consumerSource())), options);
        assertSchemas(output, "fields");
    }

    @ParameterizedTest
    @ValueSource(strings = {"load(\"hand.avsc\")", "new FakeParser().parse(\"hand.avsc\")",
            "load(\"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"Event\\\",\\\"fields\\\":[]}\")",
            "new FakeParser().parse(\"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"Event\\\",\\\"fields\\\":[]}\")",
            "new org.apache.avro.Schema.Parser().parse(\"hand.avsc\")",
            "new org.apache.avro.Schema.Parser().parse(\"\\\"string\\\"\")"})
    void unsupportedSourceCallsAndInvalidJsonNeverBecomeSchemaResources(String initializer) throws Exception {
        String event = eventSource("fields", "registry");
        int start = event.indexOf(" = new org.apache.avro.Schema.Parser()");
        event = event.substring(0, start) + " = " + initializer + event.substring(event.indexOf(';', start));
        event = event.replace("public static org.apache.avro.Schema getClassSchema()", """
                private static org.apache.avro.Schema load(String name) { throw new AssertionError(name); }
                public static class FakeParser {
                    public org.apache.avro.Schema parse(String name) { throw new AssertionError(name); }
                }
                public static org.apache.avro.Schema getClassSchema()
                """);
        Path output = directory.resolve("invalid-source");
        String diagnostics = compile(output, List.of(source("fixture/Outer.java", event), source("Consumer.java", consumerSource())),
                processorOptions(directory.resolve("absent")));
        assertThat(diagnostics).contains("No avro schema file generated");
        assertNoSchemas(output);
    }

    @Test
    void sourceTypeShadowingTheOrgPackageIsNotAcceptedAsAvroParser() throws Exception {
        String event = eventSource("fields", "registry").replace("public class Outer {", """
                public class Outer {
                    public static class org {
                        public static class apache {
                            public static class avro {
                                public static class Schema {
                                    public static class Parser {
                                        public Schema parse(String json) { throw new AssertionError("fake parser"); }
                                    }
                                }
                            }
                        }
                    }
                """);
        Path output = directory.resolve("shadowed-package");
        compile(output, List.of(source("fixture/Outer.java", event), source("Consumer.java", consumerSource())),
                processorOptions(directory.resolve("absent")));
        assertNoSchemas(output);
    }

    @Test
    void duplicateFailedTypeDoesNotPreventAnotherTypeFromProducingSchema() throws Exception {
        Path dependency = directory.resolve("dependency");
        String unsupported = eventSource("slow", "registry").replace("public static class Event {", """
                public static class Event {
                    static { try { Thread.sleep(60_000); } catch (InterruptedException e) { throw new AssertionError(e); } }
                """);
        compile(dependency, List.of(source("fixture/Outer.java", unsupported),
                source("other/Outer.java", eventSource("fast", "registry").replace("package fixture;", "package other;"))), List.of("-proc:none"));
        Path output = directory.resolve("mixed");
        String diagnostics = compile(output, List.of(source("Consumer.java", consumerSource()),
                source("OtherConsumer.java", consumerSource().replace("fixture.Outer", "other.Outer")
                        .replace("class Consumer", "class OtherConsumer").replace("appName=\"app\"", "appName=\"other\""))), processorOptions(jar(dependency)));
        assertThat(diagnostics.split("Unsupported SCHEMA\\$ classfile initializer", -1)).hasSize(2);
        try (var files = Files.list(contracts(output))) {
            List<Path> schemas = files.filter(path -> path.toString().endsWith(".avsc")).toList();
            assertThat(schemas).hasSize(1);
            assertThat(Files.readString(schemas.getFirst())).contains("\"name\":\"fast\"");
        }
    }

    @Test
    void eclipseReadsJarSchemaWithoutTreesOrWarnings() throws Exception {
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(source("fixture/Outer.java", eventSource("fields", "registry"))), List.of("-proc:none"));
        Path jar = jar(dependency);
        Path output = directory.resolve("eclipse");
        String diagnostics = compile(new org.eclipse.jdt.internal.compiler.tool.EclipseCompiler(), output,
                List.of(source("Consumer.java", consumerSource())), processorOptions(jar));
        assertSchemas(output, "fields");
        assertThat(diagnostics).doesNotContain("No avro schema", "Source origin unavailable");
    }

    @Test
    void eclipseSourceWithStaleBinaryOmitsSchemaWithoutWarning() throws Exception {
        Path dependency = directory.resolve("dependency");
        Path event = source("fixture/Outer.java", eventSource("stale", "registry"));
        compile(dependency, List.of(event), List.of("-proc:none"));
        Files.writeString(event, eventSource("current", "registry"));
        Path output = directory.resolve("eclipse");
        String diagnostics = compile(new org.eclipse.jdt.internal.compiler.tool.EclipseCompiler(), output,
                List.of(event, source("Consumer.java", consumerSource())), processorOptions(jar(dependency)));
        assertNoSchemas(output);
        assertThat(diagnostics).contains("Source initializer inspection unavailable", "refusing potentially stale");
    }

    @Test
    void disabledGenerationDoesNotExtractSchema() throws Exception {
        Path dependency = directory.resolve("dependency");
        String broken = eventSource("fields", "registry").replace("public static class Event {", """
                public static class Event {
                    static { if (System.nanoTime() != 0) throw new AssertionError("must not initialize"); }
                """);
        compile(dependency, List.of(source("fixture/Outer.java", broken)), List.of("-proc:none"));
        var options = new ArrayList<>(processorOptions(dependency));
        options.add("-AmessageContractSchemaGenerationEnabled=false");
        options.add("-Werror");
        Path output = directory.resolve("disabled");
        String diagnostics = compile(output, List.of(source("Consumer.java", consumerSource())), options);
        assertThat(diagnostics).doesNotContain("No avro schema", "must not initialize", "not recognized");
        assertNoSchemas(output);
    }

    @Test
    void sourceLiteralDoesNotInitializeAvroOrApplicationLogging() throws Exception {
        Path config = directory.resolve("logback.xml");
        Path applicationLog = directory.resolve("application.log");
        Files.writeString(config, """
                <configuration>
                    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
                        <file>%s</file>
                        <encoder><pattern>%%msg%%n</pattern></encoder>
                    </appender>
                    <root level="INFO"><appender-ref ref="FILE"/></root>
                </configuration>
                """.formatted(applicationLog));
        Path log = directory.resolve("source.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dlogback.configurationFile=" + config, "-Xlog:class+init=info", "-cp", classpath(),
                getClass().getName(), directory.toString(), "source")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly().waitFor();
        assertThat(finished).isTrue();
        assertThat(process.exitValue()).as(Files.readString(log)).isZero();
        assertThat(applicationLog).doesNotExist();
        assertThat(Files.readString(log))
                .doesNotContain("Initializing 'org/apache/avro/Schema", "No SLF4J providers", "Initializing 'org/slf4j/LoggerFactory");
    }

    private Path jar(Path dependency) throws Exception {
        Path jar = directory.resolve("messages.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar)); var paths = Files.walk(dependency)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                output.putNextEntry(new JarEntry(dependency.relativize(path).toString().replace(File.separatorChar, '/')));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
        return jar;
    }

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
        setOption(shadowOptions, "-processorpath", shadow + File.pathSeparator + classpath());
        Path third = directory.resolve("shadowed");
        compile(third, List.of(source("Consumer.java", consumerSource())), shadowOptions);
        assertSchemas(third, "fields");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void currentSourceWinsOverStaleCompiledClass(boolean explicitSource) throws Exception {
        Path dependency = directory.resolve("dependency");
        Path event = source("fixture/Outer.java", eventSource("stale", "registry"));
        compile(dependency, List.of(event), List.of("-proc:none"));
        Files.writeString(event, eventSource("current", "registry"));
        Path output = directory.resolve("application");
        Path consumer = source("Consumer.java", consumerSource());
        var options = new ArrayList<>(processorOptions(dependency));
        if (!explicitSource) options.addAll(List.of("-sourcepath", directory.toString(), "-Xprefer:source", "-implicit:class"));
        compile(output, explicitSource ? List.of(event, consumer) : List.of(consumer), options);
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
    void getterIsNeverExecuted() throws Exception {
        Path dependency = directory.resolve("dependency");
        String broken = eventSource("fields", "registry").replace("return SCHEMA$;", "throw new IllegalStateException(\"broken schema factory\");");
        compile(dependency, List.of(source("fixture/Outer.java", broken)), List.of("-proc:none"));
        Path output = directory.resolve("application");
        String diagnostics = compile(output, List.of(source("Consumer.java", consumerSource())), processorOptions(dependency));
        assertThat(diagnostics).doesNotContain("broken schema factory", "No avro schema file generated");
        assertSchemas(output, "fields");
    }

    @Test
    void assertionErrorInStaticInitializerFallsBack() throws Exception {
        initializerErrorFallsBack("AssertionError");
    }

    @Test
    void processorFailureDoesNotAffectNextCompilation() throws Exception {
        Path dependency = directory.resolve("dependency");
        compile(dependency, List.of(source("fixture/Outer.java", eventSource("fields", "registry"))), List.of("-proc:none"));
        Path consumer = source("Consumer.java", consumerSource());
        Path duplicate = source("Duplicate.java", consumerSource().replace("class Consumer", "class Duplicate"));
        Path failedOutput = directory.resolve("failed");
        List<Path> duplicatedSources = List.of(consumer, duplicate);
        List<String> options = processorOptions(dependency);
        assertThatThrownBy(() -> compile(failedOutput, duplicatedSources, options))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("duplicate");
        Path output = directory.resolve("next");
        compile(output, List.of(consumer), processorOptions(dependency));
        assertSchemas(output, "fields");
    }

    @Test
    void bothProcessorsReadSchemaWithoutInitializingApplicationCode() throws Exception {
        String key = "jeap-schema-test-" + directory.getFileName();
        Path dependency = directory.resolve("dependency");
        String counted = eventSource("fields", "registry").replace("public static org.apache.avro.Schema getClassSchema()", """
                    static {
                        System.setProperty("%s", Integer.toString(Integer.parseInt(System.getProperty("%s", "0")) + 1));
                    }
                    public static org.apache.avro.Schema getClassSchema()
                """.formatted(key, key));
        compile(dependency, List.of(source("fixture/Outer.java", counted)), List.of("-proc:none"));
        var options = new ArrayList<>(processorOptions(dependency));
        setOption(options, "-processor", JeapMessageAnnotationProcessor.class.getName() + "," + SharedSchemaTemplateProcessor.class.getName());
        try {
            Path output = directory.resolve("application");
            compile(output, List.of(source("Consumer.java", consumerSource())), options);
            assertSchemas(output, "fields");
            assertThat(System.getProperty(key)).isNull();
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
        assertThat(diagnostics).contains("Unsupported SCHEMA$ classfile initializer", "No avro schema file generated")
                .doesNotContain("broken static initializer");
        assertNoSchemas(output);
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
        setOption(options, "-processor", processor.getName());
        Path output = directory.resolve("application");
        String diagnostics = compile(output, List.of(event, source("Consumer.java", consumerSource())), options);
        if (supported) {
            assertSchemas(output, "current");
        } else {
            assertThat(diagnostics).contains("Source initializer inspection unavailable", "refusing potentially stale");
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
    void repeatedJarCompilationsWriteAllSchemas() throws Exception {
        manyJarTypes();
    }

    public static void main(String[] args) throws Exception {
        var test = new IsolatedProcessorCompilationTest();
        test.directory = Path.of(args[0]);
        if (args.length > 1 && args[1].equals("source")) {
            Path output = test.directory.resolve("source-output");
            test.compile(output, List.of(test.source("fixture/Outer.java", test.eventSource("fields", "registry").replace("{\\\"type\\\"", "{ \\\"type\\\"")),
                    test.source("Consumer.java", test.consumerSource())), test.processorOptions(test.directory.resolve("absent")));
            test.assertSchemas(output, "fields");
            try (var files = Files.list(test.contracts(output))) {
                for (Path schema : files.filter(path -> path.toString().endsWith(".avsc")).toList()) {
                    assertThat(Files.readString(schema)).startsWith("{ \"type\"");
                }
            }
            // No isolated processor path: this is the setup where an application's logging
            // configuration used to be initialized by source normalization.
            Path defaultOutput = test.directory.resolve("default-processor-path");
            test.compile(defaultOutput, List.of(test.source("fixture/Outer.java", test.eventSource("fields", "registry")),
                    test.source("Consumer.java", test.consumerSource())),
                    List.of("-processor", JeapMessageAnnotationProcessor.class.getName()));
            test.assertSchemas(defaultOutput, "fields");
            Path dependency = test.directory.resolve("binary-dependency");
            test.compile(dependency, List.of(test.source("fixture/Outer.java", test.eventSource("fields", "registry"))), List.of("-proc:none"));
            Path binaryOutput = test.directory.resolve("binary-output");
            test.compile(binaryOutput, List.of(test.source("Consumer.java", test.consumerSource())), test.processorOptions(test.jar(dependency)));
            test.assertSchemas(binaryOutput, "fields");
            Path realOutput = test.directory.resolve("real-message-output");
            test.compile(realOutput, List.of(test.source("RealConsumer.java", """
                    import ch.admin.bit.jeap.messaging.annotations.*;
                    @JeapMessageConsumerContract(value=ch.admin.bit.jme.test.JmeSimpleTestEvent.TypeRef.class, appName="app")
                    public class RealConsumer {}
                    """)), List.of("-processor", JeapMessageAnnotationProcessor.class.getName()));
            try (var files = Files.list(test.contracts(realOutput))) {
                Path schema = files.filter(path -> path.toString().endsWith(".avsc")).findFirst().orElseThrow();
                assertThat(Files.readString(schema)).contains("\"name\":\"JmeSimpleTestEvent\"");
            }
        } else {
            test.manyJarTypes();
        }
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
        Path jar = jar(dependency);
        for (int i = 0; i < 5; i++) {
            Path output = directory.resolve("run" + i);
            compile(output, consumers, processorOptions(jar));
            try (var paths = Files.list(contracts(output))) {
                assertThat(paths.filter(path -> path.toString().endsWith(".avsc")).count()).isEqualTo(40);
            }
        }
    }

    private void assertNoSchemas(Path output) throws Exception {
        try (var paths = Files.list(contracts(output))) {
            List<Path> contracts = paths.toList();
            assertThat(contracts).hasSize(2).allMatch(path -> path.toString().endsWith(".json"));
            for (Path contract : contracts) {
                assertThat(JsonParser.parseString(Files.readString(contract)).getAsJsonObject().get("schemaResource").isJsonNull()).isTrue();
            }
        }
    }

    private List<String> processorOptions(Path dependency) {
        return List.of("-classpath", dependency + File.pathSeparator + classpath(),
                "-processorpath", classpath(), "-processor", JeapMessageAnnotationProcessor.class.getName());
    }

    private static void setOption(List<String> options, String name, String value) {
        options.set(options.indexOf(name) + 1, value);
    }

    private String compile(Path output, List<Path> sources, List<String> extraOptions) throws Exception {
        return compile(ToolProvider.getSystemJavaCompiler(), output, sources, extraOptions);
    }

    private String compile(JavaCompiler compiler, Path output, List<Path> sources, List<String> extraOptions) throws Exception {
        return compile(compiler, output, sources, extraOptions, null);
    }

    private String compile(JavaCompiler compiler, Path output, List<Path> sources, List<String> extraOptions, List<String> classes) throws Exception {
        Files.createDirectories(output);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<String> options = new ArrayList<>(List.of("-d", output.toString(), "-classpath", classpath()));
            options.addAll(extraOptions);
            boolean success = compiler.getTask(null, manager, diagnostics, options, classes,
                    manager.getJavaFileObjectsFromPaths(sources)).call();
            assertThat(success).as(diagnostics.getDiagnostics().toString()).isTrue();
            assertThat(diagnostics.getDiagnostics()).noneMatch(d -> d.getKind() == javax.tools.Diagnostic.Kind.WARNING
                    || d.getKind() == javax.tools.Diagnostic.Kind.MANDATORY_WARNING);
            return diagnostics.getDiagnostics().stream().map(d -> d.getMessage(java.util.Locale.ROOT))
                    .collect(java.util.stream.Collectors.joining("\n"));
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
