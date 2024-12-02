package ch.admin.bit.jeap.messaging.avro.plugin.interfaces;

import ch.admin.bit.jeap.messaging.model.MessagePayload;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class InterfaceToolTests {
    private final static String AVRO_DOMAIN_EVENT = "ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent";
    private final static String AVRO_COMMAND = "ch.admin.bit.jeap.command.avro.AvroCommand";

    private void assertInterfaceListEquals(String actual, Object... expectedInterfaces) {
        String expected = Arrays.stream(expectedInterfaces)
                .map(o -> o instanceof Class ? ((Class) o).getCanonicalName() : o.toString())
                .collect(Collectors.joining(", "));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void eventInterface() throws IOException {
        File src = new File("src/test/resources/unittest/validEvent.avsc");
        Schema schema = new Schema.Parser().parse(src);
        InterfaceTool target = new InterfaceTool();
        assertInterfaceListEquals(target.getInterfaces(schema), SpecificRecord.class, AVRO_DOMAIN_EVENT);
    }

    @Test
    void commandInterface() throws IOException {
        File src = new File("src/test/resources/unittest/validCommand.avsc");
        Schema schema = new Schema.Parser().parse(src);
        InterfaceTool target = new InterfaceTool();
        assertInterfaceListEquals(target.getInterfaces(schema), SpecificRecord.class, AVRO_COMMAND);
    }

    @Test
    void payloadInterface() throws IOException {
        File src = new File("src/test/resources/unittest/validEvent.avsc");
        Schema schema = new Schema.Parser().parse(src);
        InterfaceTool target = new InterfaceTool();
        Schema payloadSchema = schema.getField("payload").schema();
        assertInterfaceListEquals(target.getInterfaces(payloadSchema), SpecificRecord.class, MessagePayload.class);
    }

}
