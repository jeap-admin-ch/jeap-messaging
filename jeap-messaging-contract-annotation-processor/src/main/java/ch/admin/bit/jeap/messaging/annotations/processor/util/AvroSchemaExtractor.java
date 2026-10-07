package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.source.util.TreePath;
import org.apache.avro.specific.AvroGenerated;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ExecutableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.JavaFileObject;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
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
 * <p>
 * Extraction belongs to contract annotation processing so existing message artifacts need no rebuild.
 * Binary extraction reads classfile instructions, never executing application initializers. Source
 * extraction resolves the parser method in the compiler model. Both paths validate JSON record shape
 * without initializing Avro. Unknown patterns and uncertain source origins fall back to the registry.
 */
public class AvroSchemaExtractor {

    private static final String SCHEMA_FIELD = "SCHEMA$";
    // Values must never retain compiler objects: those would keep the weak keys alive.
    private static final Map<ProcessingEnvironment, Compilation> COMPILATIONS = new WeakHashMap<>();

    private static final class Compilation {
        final Set<String> sourceRoots = new HashSet<>();
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
        COMPILATIONS.remove(env);
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
        if (schemaJson.isPresent() && !SchemaJson.isRecord(schemaJson.get())) {
            failures.add("SCHEMA$ literals are not a JSON record schema");
            schemaJson = Optional.empty();
        }
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
            failures.add("Compiler Trees API unavailable: " + describeFailure(ex));
        }
        try {
            JavaFileObject file = env.getElementUtils().getFileObjectOf(type);
            if (file != null) {
                if (file.getKind() == JavaFileObject.Kind.CLASS) return Origin.BINARY;
                if (file.getKind() == JavaFileObject.Kind.SOURCE) return Origin.SOURCE;
            }
        } catch (RuntimeException | LinkageError ex) {
            failures.add("Compiler file origin unavailable: " + describeFailure(ex));
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
                failures.add("Eclipse file origin unavailable: " + describeFailure(ex));
            }
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
     * Prefer the compiler-owned class file (including JPMS). ECJ lacks getFileObjectOf, so use its
     * module-aware Filer as a fallback. Never use processor dependencies or the Maven JVM classpath.
     */
    private static Optional<String> readSchemaJsonFromClassFile(ProcessingEnvironment env, TypeElement type,
                                                               String className, List<String> failures) {
        try {
            FileObject file = null;
            try {
                JavaFileObject origin = env.getElementUtils().getFileObjectOf(type);
                if (origin != null && origin.getKind() == JavaFileObject.Kind.CLASS) file = origin;
            } catch (RuntimeException | LinkageError unavailable) {
                // Optional compiler API; the module-aware Filer fallback below works with ECJ.
            }
            if (file == null) {
                int separator = className.lastIndexOf('.');
                String packageName = separator < 0 ? "" : className.substring(0, separator);
                String fileName = className.substring(separator + 1) + ".class";
                var module = env.getElementUtils().getModuleOf(type);
                boolean named = module != null && !module.isUnnamed();
                file = env.getFiler().getResource(named ? StandardLocation.MODULE_PATH : StandardLocation.CLASS_PATH,
                        named ? module.getQualifiedName() + "/" + packageName : packageName, fileName);
            }
            try (var input = file.openInputStream()) {
                var result = SchemaClassFile.read(input.readAllBytes(), className);
                if (result.isEmpty()) failures.add("Unsupported SCHEMA$ classfile initializer; expected generated Schema.Parser.parse literals");
                return result;
            }
        } catch (Exception | LinkageError ex) {
            failures.add("Compiler classfile unavailable: " + describeFailure(ex));
            return Optional.empty();
        } finally {
            Reference.reachabilityFence(env);
        }
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
            if (!schemaFieldTree.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC, Modifier.FINAL))) {
                failures.add("Unsupported SCHEMA$ field: expected static final");
                return Optional.empty();
            }
            ExpressionTree initializer = schemaFieldTree.getInitializer();
            Trees trees = CompilerTrees.trees(processingEnv);
            TreePath path = trees.getPath(schemaField(avroGeneratedType).orElseThrow());
            String schemaJson = null;
            if (initializer instanceof MethodInvocationTree call
                    && call.getMethodSelect() instanceof MemberSelectTree select
                    && select.getExpression() instanceof NewClassTree allocation
                    && allocation.getClassBody() == null && allocation.getArguments().isEmpty()
                    && trees.getElement(TreePath.getPath(path, allocation)) instanceof ExecutableElement constructor
                    && constructor.getEnclosingElement() instanceof TypeElement parserType
                    && parserType.getQualifiedName().contentEquals("org.apache.avro.Schema.Parser")
                    && trees.getElement(TreePath.getPath(path, call.getMethodSelect())) instanceof ExecutableElement method
                    && method.getSimpleName().contentEquals("parse")
                    && method.getEnclosingElement() instanceof TypeElement owner
                    && owner.getQualifiedName().contentEquals("org.apache.avro.Schema.Parser")) {
                schemaJson = parsedStringLiterals(initializer);
            }
            if (schemaJson == null) {
                failures.add("Missing or unsupported SCHEMA$ initializer: expected parse with string literals");
                return Optional.empty();
            }
            // Validate JSON separately without initializing the processor's Avro/logging stack.
            return Optional.of(schemaJson);
        } catch (Exception | LinkageError ex) { // NOSONAR: the compiler tree api might not be available
            failures.add("Source initializer inspection unavailable; refusing potentially stale bytecode: " + describeFailure(ex));
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

    private static String parsedStringLiterals(ExpressionTree initializer) {
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
