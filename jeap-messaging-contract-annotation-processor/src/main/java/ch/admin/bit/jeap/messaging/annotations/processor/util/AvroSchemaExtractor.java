package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import org.apache.avro.Schema;
import org.apache.avro.specific.AvroGenerated;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * Extracts the avro schema of a message type from the <code>SCHEMA$</code> field of the avro generated class.
 * <p>
 * The schema is read from the compiled class on the classpath, which is the usual case as message types are provided
 * as artifacts. If the class is not available as bytecode because it is compiled in the same compilation run as the
 * annotated class, the schema json is read from the string literals of the <code>SCHEMA$</code> field initializer
 * using the compiler tree api.
 */
public class AvroSchemaExtractor {

    private static final String SCHEMA_FIELD = "SCHEMA$";
    private static final String GET_CLASS_SCHEMA_METHOD = "getClassSchema";

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
        return extractSchemaJson(processingEnv, avroGeneratedType, null);
    }

    public static Optional<String> extractSchemaJson(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType,
                                                     TypeElement typeRef) {
        String className = processingEnv.getElementUtils().getBinaryName(avroGeneratedType).toString();
        List<Throwable> failures = new ArrayList<>();

        // Never use stale bytecode for a type being compiled in this run.
        boolean sourceAvailable = hasSourceTree(processingEnv, avroGeneratedType);
        Optional<String> schemaJson = sourceAvailable
                ? readSchemaJsonFromSourceTree(processingEnv, avroGeneratedType, failures)
                : readSchemaJsonFromClasspath(processingEnv, className, typeRef, failures);
        if (schemaJson.isEmpty()) {
            if (!sourceAvailable) {
                failures.add(new IllegalStateException("No source tree available for " + className + "; cannot extract its SCHEMA$ initializer"));
            }
            reportFailures(processingEnv, className, failures);
        }
        return schemaJson;
    }

    private static boolean hasSourceTree(ProcessingEnvironment env, TypeElement type) {
        try {
            return Trees.instance(env).getPath(type) != null;
        } catch (IllegalArgumentException | LinkageError ex) {
            return false;
        }
    }

    /**
     * Reports why the schema could not be read, as the reason is not obvious: the class might be missing on the
     * classpath, fail to initialize or be loaded by a classloader with another avro version.
     */
    private static void reportFailures(ProcessingEnvironment processingEnv, String className, List<Throwable> failures) {
        failures.stream()
                .map(AvroSchemaExtractor::failureDescription)
                .distinct()
                .forEach(failure -> processingEnv.getMessager().printMessage(NOTE,
                        "Reading " + className + "." + SCHEMA_FIELD + " failed: " + failure));
    }

    private static String failureDescription(Throwable failure) {
        StringBuilder description = new StringBuilder(failure.toString());
        while (failure.getCause() != null && failure.getCause() != failure) {
            failure = failure.getCause();
            description.append("; caused by: ").append(failure);
        }
        return description.toString();
    }

    /**
     * Resolve through the compiler's CLASS_PATH, never through processor dependencies or the Maven JVM classpath.
     */
    private static Optional<String> readSchemaJsonFromClasspath(ProcessingEnvironment env, String className,
                                                               TypeElement typeRef, List<Throwable> failures) {
        try (CompilerClasspathLoader classPathClassLoader = new CompilerClasspathLoader(env.getFiler())) {
            if (typeRef != null) {
                Class<?> metadata = Class.forName(env.getElementUtils().getBinaryName(typeRef).toString(), true, classPathClassLoader);
                for (Element element : typeRef.getEnclosedElements()) {
                    if (element instanceof VariableElement field && field.getConstantValue() instanceof String expected
                            && !expected.equals(metadata.getField(field.getSimpleName().toString()).get(null))) {
                        throw new IllegalStateException("Compiled TypeRef metadata differs from compiler model: " + field);
                    }
                }
            }
            return readSchemaJsonFromClassLoader(className, classPathClassLoader, failures);
        } catch (Exception | LinkageError ex) { // NOSONAR: the schema is optional, any failure leads to the fallback
            failures.add(ex);
            return Optional.empty();
        }
    }

    private static Optional<String> readSchemaJsonFromClassLoader(String className, ClassLoader classLoader, List<Throwable> failures) {
        if (classLoader == null) {
            return Optional.empty();
        }
        try {
            Class<?> avroGeneratedClass = Class.forName(className, true, classLoader);
            return Optional.of(readSchema(avroGeneratedClass).toString());
        } catch (Exception | LinkageError ex) { // NOSONAR: the schema is optional, any failure leads to the fallback
            failures.add(ex);
            return Optional.empty();
        }
    }

    private static Object readSchema(Class<?> avroGeneratedClass) throws Exception {
        try {
            Method getClassSchema = avroGeneratedClass.getMethod(GET_CLASS_SCHEMA_METHOD);
            return getClassSchema.invoke(null);
        } catch (NoSuchMethodException ex) {
            Field schemaField = avroGeneratedClass.getDeclaredField(SCHEMA_FIELD);
            return schemaField.get(null);
        }
    }

    /**
     * Reads the schema json from the string literals of the <code>SCHEMA$</code> field initializer, which avro
     * generates as <code>new Schema.Parser().parse("...", "...")</code>.
     */
    private static Optional<String> readSchemaJsonFromSourceTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType,
                                                                 List<Throwable> failures) {
        try {
            VariableTree schemaFieldTree = schemaFieldTree(processingEnv, avroGeneratedType);
            if (schemaFieldTree == null) {
                failures.add(new IllegalStateException("No source tree for SCHEMA$ field"));
                return Optional.empty();
            }
            String schemaJson = parsedStringLiterals(schemaFieldTree.getInitializer());
            if (schemaJson == null) {
                failures.add(new IllegalStateException("Missing or unsupported SCHEMA$ initializer: expected parse with string literals"));
                return Optional.empty();
            }
            return Optional.of(new Schema.Parser().parse(schemaJson).toString());
        } catch (Exception | LinkageError ex) { // NOSONAR: the compiler tree api might not be available
            failures.add(ex);
            return Optional.empty();
        }
    }

    private static VariableTree schemaFieldTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType) {
        Optional<? extends Element> schemaField = schemaField(avroGeneratedType);
        if (schemaField.isEmpty()) {
            return null;
        }
        Tree tree = Trees.instance(processingEnv).getTree(schemaField.get());
        return tree instanceof VariableTree variableTree ? variableTree : null;
    }

    private static Optional<? extends Element> schemaField(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD)
                .filter(element -> SCHEMA_FIELD.equals(element.getSimpleName().toString()))
                .findFirst();
    }

    static String parsedStringLiterals(ExpressionTree initializer) {
        if (!(initializer instanceof MethodInvocationTree methodInvocation)) {
            return null;
        }
        StringBuilder schemaJson = new StringBuilder();
        for (ExpressionTree argument : methodInvocation.getArguments()) {
            if (!(argument instanceof LiteralTree literal) || !(literal.getValue() instanceof String stringLiteral)) {
                return null;
            }
            schemaJson.append(stringLiteral);
        }
        return schemaJson.isEmpty() ? null : schemaJson.toString();
    }
}
