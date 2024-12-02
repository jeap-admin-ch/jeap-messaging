package ch.admin.bit.jeap.messaging.avro;

import lombok.experimental.UtilityClass;
import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@UtilityClass
public class AvroSerializationHelper {
    public static byte[] serialize(AvroMessage event) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            Schema schema = event.getSchema();
            DatumWriter<AvroMessage> writer = new SpecificDatumWriter<>(schema);
            writer.write(event, encoder);
            encoder.flush();
            return out.toByteArray();
        }
    }

    public static <T extends AvroMessage> T deserialize(byte[] message, Class<T> eventClass)
            throws ReflectiveOperationException, IOException {
        Schema schema = (eventClass.getDeclaredConstructor().newInstance()).getSchema();
        DatumReader<T> reader = new SpecificDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(message, null);
        return reader.read(null, decoder);
    }
}
