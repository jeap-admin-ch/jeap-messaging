package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;

import java.io.File;
import java.io.IOException;

/**
 * Parser converting IDL-Files into Avro-Protocols. As Avro-Files can integrate other files we need to parser to
 * be able to find them as well.
 */
public class IdlFileParser {
    private final ImportClassLoader classLoader;

    /**
     * Create a new IDL-Parser
     *
     * @param classLoader A class loader to load files imported by the schema
     */
    public IdlFileParser(ImportClassLoader classLoader) {
        this.classLoader = classLoader;
    }

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
