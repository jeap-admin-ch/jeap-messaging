package ch.admin.bit.jeap.messaging.avro.plugin.interfaces;

import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;

@RequiredArgsConstructor
public class InterfaceTool {

    //This method is used in the velocity template
    @SuppressWarnings("unused")
    public String getInterfaces(Schema schema) {
        return InterfaceList.builder()
                .schema(schema)
                .build();
    }
}
