package ch.admin.bit.jeap.messaging.registry.verifier.common;

import org.apache.avro.compiler.idl.IdlConstants;
import org.apache.avro.compiler.idl.IdlTokenManager;
import org.apache.avro.compiler.idl.JavaCharStream;
import org.apache.avro.compiler.idl.Token;
import org.apache.avro.compiler.idl.TokenMgrError;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The tokens of an Avro IDL file, read with the lexer of Avro's own IDL parser. Comments are not part of the token
 * stream and whitespace only separates tokens, so analyses based on these tokens do not depend on comments or
 * formatting.
 */
final class IdlTokens {

    private final List<Token> tokens;

    private IdlTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    static IdlTokens read(InputStream inputStream) throws IOException {
        // Decode leniently: existing schema files may contain bytes that are not valid UTF-8 in comments
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        IdlTokenManager tokenManager = new IdlTokenManager(new JavaCharStream(reader));
        List<Token> tokens = new ArrayList<>();
        try {
            for (Token token = tokenManager.getNextToken(); token.kind != IdlConstants.EOF; token = tokenManager.getNextToken()) {
                tokens.add(token);
            }
        } catch (TokenMgrError e) {
            throw new IOException("Invalid Avro IDL: " + e.getMessage(), e);
        }
        return new IdlTokens(tokens);
    }

    /**
     * @return the files imported with {@code import idl "<file>";}
     */
    Set<String> idlImports() {
        Set<String> imports = new LinkedHashSet<>();
        for (int i = 0; i + 2 < tokens.size(); i++) {
            if (is(i, IdlConstants.IMPORT) && is(i + 1, IdlConstants.IDL) && is(i + 2, IdlConstants.STRING_LITERAL)) {
                imports.add(unquote(tokens.get(i + 2).image));
            }
        }
        return imports;
    }

    /**
     * @return the names used within the bodies of records and errors, each both as written (possibly
     * qualified) and as simple name
     */
    Set<String> namesUsedInRecords() {
        Set<String> names = new LinkedHashSet<>();
        int i = 0;
        while (i < tokens.size()) {
            if (isTypeDeclaration(i, IdlConstants.RECORD, IdlConstants.ERROR)) {
                i = collectNamesInBody(indexOf(IdlConstants.LBRACE, i), names);
            } else {
                i++;
            }
        }
        return names;
    }

    /**
     * @return the named types (records, errors, enums, fixed) declared in this file
     */
    Set<AvroTypeInfo> declaredTypes() {
        Set<AvroTypeInfo> types = new LinkedHashSet<>();
        String protocolNamespace = "";
        String namespaceAnnotation = null;
        int i = 0;
        while (i < tokens.size()) {
            if (isNamespaceAnnotation(i)) {
                namespaceAnnotation = unquote(tokens.get(i + 3).image);
                // continue after the closing parenthesis
                i += 4;
            } else if (is(i, IdlConstants.PROTOCOL) && !is(i - 1, IdlConstants.IMPORT)) {
                protocolNamespace = namespaceAnnotation == null ? "" : namespaceAnnotation;
                namespaceAnnotation = null;
            } else if (isTypeDeclaration(i, IdlConstants.RECORD, IdlConstants.ERROR, IdlConstants.ENUM, IdlConstants.FIXED)) {
                String name = nameAt(i + 1);
                String namespace = namespaceAnnotation == null ? protocolNamespace : namespaceAnnotation;
                namespaceAnnotation = null;
                int lastDot = name.lastIndexOf('.');
                if (lastDot >= 0) {
                    // A qualified name overrides any namespace
                    namespace = name.substring(0, lastDot);
                    name = name.substring(lastDot + 1);
                }
                types.add(new AvroTypeInfo(namespace, name));
            }
            i++;
        }
        return types;
    }

    /**
     * Collects the names in the body starting at the given opening brace and returns the index after its closing
     * brace. Nested braces (unions, default values) are part of the body.
     */
    private int collectNamesInBody(int openingBrace, Set<String> names) {
        int depth = 0;
        int i = openingBrace;
        while (i < tokens.size()) {
            if (is(i, IdlConstants.LBRACE)) {
                depth++;
            } else if (is(i, IdlConstants.RBRACE)) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            } else if (is(i, IdlConstants.IDENTIFIER) && !is(i - 1, IdlConstants.AT)) {
                String name = nameAt(i);
                names.add(name);
                names.add(name.substring(name.lastIndexOf('.') + 1));
                // continue after the (possibly qualified) name
                i += 2 * (int) name.chars().filter(c -> c == '.').count();
            }
            i++;
        }
        return i;
    }

    /**
     * @return the name starting at the given index: a possibly qualified identifier, or any token quoted in
     * backticks (used to declare names that are keywords)
     */
    private String nameAt(int index) {
        if (is(index, IdlConstants.TICK)) {
            return index + 1 < tokens.size() ? tokens.get(index + 1).image : "";
        }
        StringBuilder name = new StringBuilder();
        int i = index;
        while (is(i, IdlConstants.IDENTIFIER)) {
            name.append(tokens.get(i).image);
            if (is(i + 1, IdlConstants.DOT) && is(i + 2, IdlConstants.IDENTIFIER)) {
                name.append('.');
                i += 2;
            } else {
                break;
            }
        }
        return name.toString();
    }

    private boolean isTypeDeclaration(int index, int... declarationKinds) {
        // A keyword in backticks is a name, not a declaration
        if (is(index - 1, IdlConstants.TICK)) {
            return false;
        }
        for (int kind : declarationKinds) {
            if (is(index, kind)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNamespaceAnnotation(int index) {
        return is(index, IdlConstants.AT) &&
               is(index + 1, IdlConstants.IDENTIFIER) && "namespace".equals(tokens.get(index + 1).image) &&
               is(index + 2, IdlConstants.LPAREN) &&
               is(index + 3, IdlConstants.STRING_LITERAL) &&
               is(index + 4, IdlConstants.RPAREN);
    }

    private int indexOf(int kind, int from) {
        for (int i = from; i < tokens.size(); i++) {
            if (is(i, kind)) {
                return i;
            }
        }
        return tokens.size();
    }

    private boolean is(int index, int kind) {
        return index >= 0 && index < tokens.size() && tokens.get(index).kind == kind;
    }

    private static String unquote(String stringLiteral) {
        return stringLiteral.substring(1, stringLiteral.length() - 1);
    }
}
