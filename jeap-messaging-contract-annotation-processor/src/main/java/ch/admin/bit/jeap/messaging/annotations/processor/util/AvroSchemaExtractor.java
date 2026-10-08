package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.apache.avro.specific.AvroGenerated;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * Extracts the avro schema of a message type from the <code>SCHEMA$</code> field of the avro generated class.
 * <p>
 * The schema is read from the compiled class on the classpath, which is the usual case as message types are provided
 * as artifacts. If the class is not available as bytecode because it is compiled in the same compilation run as the
 * annotated class, the schema json is read from the string literals of the <code>SCHEMA$</code> field initializer
 * using the compiler tree api.
 * <p>
 * Extraction belongs to contract annotation processing so existing message artifacts need no rebuild.
 * Binary extraction reads classfile instructions, never executing application initializers. Source
 * extraction recognizes the fully qualified generated initializer without attributing expressions.
 * Both paths validate JSON record shape without initializing Avro. Unknown patterns and uncertain
 * source origins fall back to the registry.
 */
public class AvroSchemaExtractor {

    private static final String SCHEMA_FIELD = "SCHEMA$";

    private AvroSchemaExtractor() {
    }

    /**
     * Returns true if the given type looks like an avro generated class, i.e. carries the
     * {@link org.apache.avro.specific.AvroGenerated} annotation or declares a <code>SCHEMA$</code> field. Checked on
     * the element model only, without loading the class.
     */
    public static boolean isAvroGeneratedType(TypeElement typeElement) {
        return typeElement.getAnnotation(AvroGenerated.class) != null || schemaField(typeElement).isPresent();
    }

    public static Optional<String> extractSchemaJson(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType) {
        String className = processingEnv.getElementUtils().getBinaryName(avroGeneratedType).toString();
        List<String> failures = new ArrayList<>();

        // Never use stale bytecode for a type being compiled in this run.
        Origin origin = origin(processingEnv, avroGeneratedType, failures);
        Optional<String> schemaJson = switch (origin) {
            case SOURCE -> readSchemaJsonFromSourceTree(processingEnv, avroGeneratedType, failures);
            case BINARY -> readSchemaJsonFromClassFile(processingEnv, avroGeneratedType, className, failures);
            case UNAVAILABLE -> {
                failures.add("Source origin unavailable; refusing potentially stale CLASS_PATH bytecode");
                yield Optional.empty();
            }
        };
        if (schemaJson.isPresent()) {
            try {
                if (!SchemaJson.isRecord(schemaJson.get())) {
                    failures.add("SCHEMA$ literals are not a JSON record schema");
                    schemaJson = Optional.empty();
                }
            } catch (LinkageError unavailable) {
                failures.add("Schema JSON validation unavailable; check the Gson dependency on the processor path: " + describeFailure(unavailable));
                schemaJson = Optional.empty();
            }
        }
        if (schemaJson.isEmpty()) {
            reportFailures(processingEnv, className, failures);
        }
        return schemaJson;
    }

    private enum Origin { SOURCE, BINARY, UNAVAILABLE }

    private static Origin origin(ProcessingEnvironment env, TypeElement type, List<String> failures) {
        try {
            JavaFileObject file = env.getElementUtils().getFileObjectOf(type);
            if (file != null) {
                if (file.getKind() == JavaFileObject.Kind.CLASS) return Origin.BINARY;
                if (file.getKind() == JavaFileObject.Kind.SOURCE) return Origin.SOURCE;
            }
        } catch (RuntimeException | LinkageError ex) {
            failures.add("Compiler file origin unavailable: " + describeFailure(ex));
        }
        return eclipseFileOrigin(type, failures);
    }

    /**
     * ECJ cannot reliably supply files through getFileObjectOf in all environments (notably JARs). Its element
     * exposes the binding's actual file, including implicitly loaded source.
     * <p>
     * The element class is matched by name on purpose: ECJ is not a compile dependency of this processor, so
     * {@code instanceof} is not available here. Keeping the name check confines the reflective adapter to that
     * one compiler instead of calling an unrelated {@code getFileName()} on any other implementation.
     */
    @SuppressWarnings("java:S1872") // see javadoc: ECJ is not a compile dependency, instanceof is impossible
    private static Origin eclipseFileOrigin(TypeElement type, List<String> failures) {
        if (!type.getClass().getName().equals("org.eclipse.jdt.internal.compiler.apt.model.TypeElementImpl")) {
            return Origin.UNAVAILABLE;
        }
        try {
            Object file = type.getClass().getMethod("getFileName").invoke(type);
            if (file instanceof String name) {
                if (name.endsWith(".class")) return Origin.BINARY;
                if (name.endsWith(".java")) return Origin.SOURCE;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            failures.add("Eclipse file origin unavailable: " + describeFailure(ex));
        }
        return Origin.UNAVAILABLE;
    }

    /**
     * Reports unavailable compiler classfiles or source trees, unsupported initializers and invalid
     * schema literals. Wrapped compiler and I/O failures include their complete cause chain.
     */
    private static void reportFailures(ProcessingEnvironment processingEnv, String className, List<String> failures) {
        failures.stream()
                .distinct()
                .forEach(failure -> processingEnv.getMessager().printMessage(NOTE,
                        "Reading " + className + "." + SCHEMA_FIELD + " failed: " + failure));
    }

    private static String describeFailure(Throwable failure) {
        StringBuilder description = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (failure != null && seen.add(failure)) {
            if (!description.isEmpty()) {
                description.append("; caused by: ");
            }
            description.append(failure);
            failure = failure.getCause();
        }
        return description.toString();
    }

    /**
     * Prefer the compiler-owned class file (including JPMS), with module-aware Filer access as a
     * fallback for compilers such as ECJ. Never use processor dependencies or the Maven JVM classpath.
     */
    private static Optional<String> readSchemaJsonFromClassFile(ProcessingEnvironment env, TypeElement type,
                                                               String className, List<String> failures) {
        try (var input = compilerClassFile(env, type, className).openInputStream()) {
            var result = SchemaClassFile.read(input.readAllBytes(), className);
            if (result.isEmpty()) failures.add("Unsupported SCHEMA$ classfile initializer; expected generated Schema.Parser.parse literals");
            return result;
        } catch (Exception | LinkageError ex) {
            failures.add("Compiler classfile unavailable: " + describeFailure(ex));
            return Optional.empty();
        }
    }

    /** Resolves the compiler-owned class file, preferring the optional element API over the Filer. */
    private static FileObject compilerClassFile(ProcessingEnvironment env, TypeElement type, String className) throws IOException {
        FileObject file = elementClassFile(env, type);
        if (file != null) {
            return file;
        }
        int separator = className.lastIndexOf('.');
        String packageName = separator < 0 ? "" : className.substring(0, separator);
        String fileName = className.substring(separator + 1) + ".class";
        var module = env.getElementUtils().getModuleOf(type);
        boolean named = module != null && !module.isUnnamed();
        return env.getFiler().getResource(named ? StandardLocation.MODULE_PATH : StandardLocation.CLASS_PATH,
                named ? module.getQualifiedName() + "/" + packageName : packageName, fileName);
    }

    private static JavaFileObject elementClassFile(ProcessingEnvironment env, TypeElement type) {
        try {
            JavaFileObject origin = env.getElementUtils().getFileObjectOf(type);
            return origin != null && origin.getKind() == JavaFileObject.Kind.CLASS ? origin : null;
        } catch (RuntimeException | LinkageError unavailable) {
            // Optional compiler API; the module-aware Filer fallback works with ECJ.
            return null;
        }
    }

    /**
     * Reads the schema json from the string literals of the <code>SCHEMA$</code> field initializer, which avro
     * generates as <code>new org.apache.avro.Schema.Parser().parse("...", "...")</code>.
     */
    private static Optional<String> readSchemaJsonFromSourceTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType,
                                                                 List<String> failures) {
        try {
            // The adapter's signature contains no javac types. Resolve it only for source extraction,
            // within this LinkageError boundary, so ECJ also works without the jdk.compiler module.
            Optional<String> schema = SourceSchemaReader.read(processingEnv, avroGeneratedType);
            if (schema.isEmpty()) {
                failures.add("Missing or unsupported SCHEMA$ initializer: expected fully qualified org.apache.avro.Schema.Parser.parse with string literals");
            }
            return schema;
        } catch (Exception | LinkageError ex) { // NOSONAR: the compiler tree api might not be available
            failures.add("Source initializer inspection unavailable; refusing potentially stale bytecode: " + describeFailure(ex));
            return Optional.empty();
        }
    }

    private static Optional<? extends Element> schemaField(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .filter(element -> SCHEMA_FIELD.equals(element.getSimpleName().toString()))
                .findFirst();
    }

}
