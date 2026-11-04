package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import lombok.RequiredArgsConstructor;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.avro.compiler.idl.TokenMgrError;

import java.io.File;
import java.io.IOException;

/**
 * Parser converting IDL-Files into Avro-Protocols. As Avro-Files can integrate other files we need to parser to
 * be able to find them as well.
 */
@RequiredArgsConstructor
public class IdlFileParser {
    private final ImportClassLoader classLoader;

    /**
     * Parses an IDL-File
     *
     * @param src The IDL-File to parse
     * @return The parsed Protocol
     * @throws IOException    If the input file cannot be read
     * @throws ParseException If the input file cannot be parsed, including lexical errors such as an unterminated
     *                        comment or an invalid unicode escape
     */
    public Protocol parseIdlFile(File src) throws IOException, ParseException {
        try (Idl parser = new Idl(src, classLoader)) {
            Protocol p = parser.CompilationUnit();
            String json = p.toString(true);
            return Protocol.parse(json);
        } catch (Error e) { // NOSONAR only lexical errors are converted, see isLexicalError
            if (!isLexicalError(e)) {
                throw e;
            }
            ParseException parseException = new ParseException(src.getAbsolutePath() + ": " + e.getMessage());
            parseException.initCause(e);
            throw parseException;
        }
    }

    /**
     * Avro's IDL lexer reports invalid input with an {@link Error}, which callers handling parse failures would not
     * catch: a {@link TokenMgrError} for invalid tokens such as an unterminated comment, and a plain {@link Error} from
     * {@code JavaCharStream} for an invalid unicode escape, which is decoded even in comments (e.g. a Windows path).
     */
    public static boolean isLexicalError(Error error) {
        return error instanceof TokenMgrError || error.getClass() == Error.class;
    }
}
