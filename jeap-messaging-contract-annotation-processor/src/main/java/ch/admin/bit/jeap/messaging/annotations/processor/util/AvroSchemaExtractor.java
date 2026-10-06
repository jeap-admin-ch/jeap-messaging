package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import org.apache.avro.specific.AvroGenerated;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.JavaFileObject;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

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
    private static final Cleaner CLEANER = Cleaner.create();
    // Values must never retain compiler objects: those would keep the weak keys alive.
    private static final Map<ProcessingEnvironment, Compilation> COMPILATIONS = new WeakHashMap<>();

    private static final class Compilation {
        final Set<String> sourceRoots = new HashSet<>();
        SchemaExtractionWorker worker;
        Cleaner.Cleanable cleanup;
    }

    public static synchronized void round(ProcessingEnvironment env, Set<? extends Element> roots) {
        Compilation compilation = COMPILATIONS.computeIfAbsent(env, ignored -> new Compilation());
        for (Element root : roots) {
            if (root instanceof TypeElement type) {
                compilation.sourceRoots.add(type.getQualifiedName().toString());
            }
        }
    }

    public static synchronized void processingOver(ProcessingEnvironment env) {
        Compilation compilation = COMPILATIONS.remove(env);
        if (compilation != null && compilation.cleanup != null) {
            compilation.cleanup.clean();
        }
    }

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
        List<String> failures = new ArrayList<>();

        // Never use stale bytecode for a type being compiled in this run.
        Origin origin = origin(processingEnv, avroGeneratedType, failures);
        Optional<String> schemaJson = switch (origin) {
            case SOURCE -> readSchemaJsonFromSourceTree(processingEnv, avroGeneratedType, failures);
            case BINARY -> readSchemaJsonFromClasspath(processingEnv, className, typeRef, failures);
            case UNAVAILABLE -> {
                failures.add("Source origin unavailable; refusing potentially stale CLASS_PATH bytecode");
                yield Optional.empty();
            }
        };
        if (schemaJson.isEmpty()) {
            reportFailures(processingEnv, className, failures);
        }
        return schemaJson;
    }

    private enum Origin { SOURCE, BINARY, UNAVAILABLE }

    private static Origin origin(ProcessingEnvironment env, TypeElement type, List<String> failures) {
        synchronized (AvroSchemaExtractor.class) {
            Compilation compilation = COMPILATIONS.get(env);
            for (Element enclosing = type; enclosing instanceof TypeElement current; enclosing = enclosing.getEnclosingElement()) {
                if (compilation != null && compilation.sourceRoots.contains(current.getQualifiedName().toString())) {
                    return Origin.SOURCE;
                }
            }
        }
        try {
            Trees trees = CompilerTrees.trees(env);
            if (trees.getPath(type) != null) {
                return Origin.SOURCE;
            }
            // A working Trees API and a compiler-owned element with no path denotes a binary.
            return env.getElementUtils().getTypeElement(type.getQualifiedName()) == type ? Origin.BINARY : Origin.UNAVAILABLE;
        } catch (RuntimeException | LinkageError ex) {
            failures.add("Compiler Trees API unavailable: " + ex);
        }
        try {
            JavaFileObject file = env.getElementUtils().getFileObjectOf(type);
            if (file != null) {
                if (file.getKind() == JavaFileObject.Kind.CLASS) return Origin.BINARY;
                if (file.getKind() == JavaFileObject.Kind.SOURCE) return Origin.SOURCE;
            }
        } catch (RuntimeException | LinkageError ex) {
            failures.add("Compiler file origin unavailable: " + ex);
        }
        // ECJ 3.43 does not implement Elements.getFileObjectOf yet. Its element exposes the
        // binding's actual file, including implicitly loaded source; absence from round roots alone
        // is NOT proof of a binary. Keep this optional adapter separate from compiler dependencies.
        if (type.getClass().getName().equals("org.eclipse.jdt.internal.compiler.apt.model.TypeElementImpl")) {
            try {
                Object file = type.getClass().getMethod("getFileName").invoke(type);
                if (file instanceof String name) {
                    if (name.endsWith(".class")) return Origin.BINARY;
                    if (name.endsWith(".java")) return Origin.SOURCE;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
                failures.add("Eclipse file origin unavailable: " + ex);
            }
        }
        return Origin.UNAVAILABLE;
    }

    /**
     * Reports why the schema could not be read, as the reason is not obvious: the class might be missing on the
     * classpath, fail to initialize or be loaded by a classloader with another avro version.
     */
    private static void reportFailures(ProcessingEnvironment processingEnv, String className, List<String> failures) {
        failures.stream()
                .distinct()
                .forEach(failure -> processingEnv.getMessager().printMessage(NOTE,
                        "Reading " + className + "." + SCHEMA_FIELD + " failed: " + failure));
    }

    /**
     * Resolve through the compiler's CLASS_PATH, never through processor dependencies or the Maven JVM classpath.
     */
    private static Optional<String> readSchemaJsonFromClasspath(ProcessingEnvironment env, String className,
                                                               TypeElement typeRef, List<String> failures) {
        Map<String, String> constants = new HashMap<>();
        String metadataName = null;
        if (typeRef != null) {
            metadataName = env.getElementUtils().getBinaryName(typeRef).toString();
            for (Element element : typeRef.getEnclosedElements()) {
                if (element instanceof VariableElement field && field.getConstantValue() instanceof String expected) {
                    constants.put(field.getSimpleName().toString(), expected);
                }
            }
        }
        try {
            var result = worker(env).extract(env.getFiler(), className, metadataName, Map.copyOf(constants));
            if (result.diagnostic() != null) {
                failures.add(result.diagnostic());
            }
            return Optional.ofNullable(result.schema());
        } catch (RuntimeException | Error ex) {
            processingOver(env);
            throw ex;
        } finally {
            Reference.reachabilityFence(env);
        }
    }

    private static synchronized SchemaExtractionWorker worker(ProcessingEnvironment env) {
        Compilation compilation = COMPILATIONS.computeIfAbsent(env, ignored -> new Compilation());
        if (compilation.worker == null) {
            compilation.worker = new SchemaExtractionWorker();
            compilation.cleanup = CLEANER.register(env, compilation.worker);
        }
        return compilation.worker;
    }

    /**
     * Reads the schema json from the string literals of the <code>SCHEMA$</code> field initializer, which avro
     * generates as <code>new Schema.Parser().parse("...", "...")</code>.
     */
    private static Optional<String> readSchemaJsonFromSourceTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType,
                                                                 List<String> failures) {
        try {
            VariableTree schemaFieldTree = schemaFieldTree(processingEnv, avroGeneratedType);
            if (schemaFieldTree == null) {
                failures.add("No source tree for SCHEMA$ field");
                return Optional.empty();
            }
            String schemaJson = parsedStringLiterals(schemaFieldTree.getInitializer());
            if (schemaJson == null) {
                failures.add("Missing or unsupported SCHEMA$ initializer: expected parse with string literals");
                return Optional.empty();
            }
            // This is already the JSON supplied to SCHEMA$. Do not initialize the processor's
            // Avro/logging stack merely to normalize it. MCS validates the uploaded schema.
            return Optional.of(schemaJson);
        } catch (Exception | LinkageError ex) { // NOSONAR: the compiler tree api might not be available
            processingOver(processingEnv);
            failures.add("Source initializer inspection unavailable; refusing potentially stale bytecode: " + ex);
            return Optional.empty();
        } catch (Error fatal) {
            processingOver(processingEnv);
            throw fatal;
        } finally {
            Reference.reachabilityFence(processingEnv);
        }
    }

    private static VariableTree schemaFieldTree(ProcessingEnvironment processingEnv, TypeElement avroGeneratedType) {
        Optional<? extends Element> schemaField = schemaField(avroGeneratedType);
        if (schemaField.isEmpty()) {
            return null;
        }
        Tree tree = CompilerTrees.trees(processingEnv).getTree(schemaField.get());
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
