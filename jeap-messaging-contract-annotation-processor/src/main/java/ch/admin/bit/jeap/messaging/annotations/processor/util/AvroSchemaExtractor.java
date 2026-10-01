package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import org.apache.avro.Schema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<String, String> SCHEMA_JSON_BY_CLASS_NAME = new ConcurrentHashMap<>();

    private static URLClassLoader classPathClassLoader;

    private AvroSchemaExtractor() {
    }

    public static Optional<String> extractSchemaJson(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType) {
        String className = avroGeneratedType.getQualifiedName().toString();
        String cachedSchemaJson = SCHEMA_JSON_BY_CLASS_NAME.get(className);
        if (cachedSchemaJson != null) {
            return Optional.of(cachedSchemaJson);
        }
        Optional<String> schemaJson = readSchemaJson(processingEnv, avroGeneratedType, className);
        schemaJson.ifPresent(json -> SCHEMA_JSON_BY_CLASS_NAME.put(className, json));
        return schemaJson;
    }

    private static Optional<String> readSchemaJson(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType, String className) {
        Optional<String> schemaJson = readSchemaJsonFromClasspath(className);
        if (schemaJson.isPresent()) {
            return schemaJson;
        }
        return readSchemaJsonFromSourceTree(processingEnv, avroGeneratedType);
    }

    private static Optional<String> readSchemaJsonFromClasspath(String className) {
        for (ClassLoader classLoader : classLoaders()) {
            Optional<String> schemaJson = readSchemaJsonFromClasspath(className, classLoader);
            if (schemaJson.isPresent()) {
                return schemaJson;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> readSchemaJsonFromClasspath(String className, ClassLoader classLoader) {
        try {
            Class<?> avroGeneratedClass = Class.forName(className, true, classLoader);
            return Optional.of(readSchema(avroGeneratedClass).toString());
        } catch (Exception | NoClassDefFoundError ex) { // NOSONAR: the schema is optional, any failure leads to the fallback
            return Optional.empty();
        }
    }

    private static Schema readSchema(Class<?> avroGeneratedClass) throws Exception {
        try {
            Method getClassSchema = avroGeneratedClass.getMethod(GET_CLASS_SCHEMA_METHOD);
            return (Schema) getClassSchema.invoke(null);
        } catch (NoSuchMethodException ex) {
            Field schemaField = avroGeneratedClass.getDeclaredField(SCHEMA_FIELD);
            return (Schema) schemaField.get(null);
        }
    }

    /**
     * The message type classes are usually visible to the classloader of the annotation processor, as the annotation
     * processor is loaded from the compile classpath. As a fallback, a classloader for the classpath of the compiling
     * jvm is used.
     */
    private static synchronized List<ClassLoader> classLoaders() {
        List<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(AvroSchemaExtractor.class.getClassLoader());
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            classLoaders.add(contextClassLoader);
        }
        if (classPathClassLoader == null) {
            try {
                classPathClassLoader = ClasspathUtils.createExtendedClassLoader();
            } catch (Exception ex) { // NOSONAR: the schema is optional, any failure leads to the fallback
                classPathClassLoader = null;
            }
        }
        if (classPathClassLoader != null) {
            classLoaders.add(classPathClassLoader);
        }
        return classLoaders;
    }

    /**
     * Reads the schema json from the string literals of the <code>SCHEMA$</code> field initializer, which avro
     * generates as <code>new Schema.Parser().parse("...", "...")</code>.
     */
    private static Optional<String> readSchemaJsonFromSourceTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType) {
        try {
            VariableTree schemaFieldTree = schemaFieldTree(processingEnv, avroGeneratedType);
            if (schemaFieldTree == null) {
                return Optional.empty();
            }
            String schemaJson = parsedStringLiterals(schemaFieldTree.getInitializer());
            if (schemaJson == null) {
                return Optional.empty();
            }
            return Optional.of(new Schema.Parser().parse(schemaJson).toString());
        } catch (Exception | NoClassDefFoundError ex) { // NOSONAR: the compiler tree api might not be available
            return Optional.empty();
        }
    }

    private static VariableTree schemaFieldTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType) {
        Trees trees = Trees.instance(processingEnv);
        for (Element enclosedElement : avroGeneratedType.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD && SCHEMA_FIELD.contentEquals(enclosedElement.getSimpleName())) {
                Tree tree = trees.getTree(enclosedElement);
                return tree instanceof VariableTree variableTree ? variableTree : null;
            }
        }
        return null;
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
