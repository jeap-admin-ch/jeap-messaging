package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import lombok.RequiredArgsConstructor;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;

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
     * @throws ParseException If the input file cannot be parsed
     */
    public Protocol parseIdlFile(File src) throws IOException, ParseException {
        try (Idl parser = new Idl(src, classLoader)) {
            Protocol p = parser.CompilationUnit();
            String json = p.toString(true);
            return Protocol.parse(json);
        }
    }
}
