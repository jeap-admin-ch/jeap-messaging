package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.Set;

/** Optional javac adapter. Never call Trees.getElement/getTypeMirror: those attribute incomplete classes. */
final class SourceSchemaReader {
    private SourceSchemaReader() { }

    static Optional<String> read(ProcessingEnvironment env, TypeElement type) {
        var field = type.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.FIELD && element.getSimpleName().contentEquals("SCHEMA$"))
                .findFirst();
        if (field.isEmpty()) return Optional.empty();
        var trees = CompilerTrees.trees(env);
        if (!(trees.getTree(field.get()) instanceof VariableTree variable)
                || !variable.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC, Modifier.FINAL))
                || !(variable.getInitializer() instanceof MethodInvocationTree call)
                || !(call.getMethodSelect() instanceof MemberSelectTree method)
                || !method.getIdentifier().contentEquals("parse")
                || !(method.getExpression() instanceof NewClassTree parser)
                || !parser.getIdentifier().toString().equals("org.apache.avro.Schema.Parser")
                || parser.getClassBody() != null || !parser.getArguments().isEmpty()
                || parser.getEnclosingExpression() != null || !parser.getTypeArguments().isEmpty()
                || !call.getTypeArguments().isEmpty()) {
            return Optional.empty();
        }
        if (mayShadowPackage(env, type, trees.getPath(field.get()))) return Optional.empty();
        StringBuilder json = new StringBuilder();
        for (var argument : call.getArguments()) {
            if (!(argument instanceof LiteralTree literal) || !(literal.getValue() instanceof String value)) return Optional.empty();
            json.append(value);
        }
        return json.isEmpty() ? Optional.empty() : Optional.of(json.toString());
    }

    /** Reject ambiguous hand-written scopes instead of resolving the initializer through javac attribution. */
    private static boolean mayShadowPackage(ProcessingEnvironment env, TypeElement type, TreePath path) {
        if (path == null) return true;
        for (var imported : path.getCompilationUnit().getImports()) {
            String name = imported.getQualifiedIdentifier().toString();
            if (name.endsWith(".org") || name.endsWith(".*")) return true;
        }
        var packageName = env.getElementUtils().getPackageOf(type).getQualifiedName();
        if (env.getElementUtils().getTypeElement(packageName.isEmpty() ? "org" : packageName + ".org") != null) return true;
        for (Element enclosing = type; enclosing instanceof TypeElement current; enclosing = enclosing.getEnclosingElement()) {
            if (current.getSimpleName().contentEquals("org")
                    || current.getTypeParameters().stream().anyMatch(parameter -> parameter.getSimpleName().contentEquals("org"))
                    || env.getElementUtils().getAllMembers(current).stream().anyMatch(member -> member.getSimpleName().contentEquals("org"))) {
                return true;
            }
        }
        return false;
    }
}
