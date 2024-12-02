package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.ParseException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdlFileParseTest {
    @Test
    void valid() throws IOException, ParseException {
        final File testDirectory = new File("src/test/resources/unittest/");
        final ImportClassLoader importClassLoader = new ImportClassLoader(new File("src/test/resources/sample-registry-event/avro"), Collections.emptyList());
        IdlFileParser target = new IdlFileParser(importClassLoader);
        Protocol protocol = target.parseIdlFile(new File(testDirectory, "valid-idl.avdl"));
        assertNotNull(protocol);
    }

    @Test
    void missingInput() {
        final File testDirectory = new File("src/test/resources/unittest/");
        IdlFileParser target = new IdlFileParser(new ImportClassLoader());
        assertThrows(ParseException.class, () -> target.parseIdlFile(new File(testDirectory, "valid-idl.avdl")));
    }
}
