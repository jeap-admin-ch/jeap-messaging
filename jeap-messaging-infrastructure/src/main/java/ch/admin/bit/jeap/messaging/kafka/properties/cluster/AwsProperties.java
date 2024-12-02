package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import lombok.Data;

@Data
public class AwsProperties {
    private GlueProperties glue;

    private MskAuthProperties msk;

    void validateProperties(boolean confluentSchemaRegistryActive) {
        if (glue != null) {
            glue.validateProperties(confluentSchemaRegistryActive);
        }
        if (msk != null) {
            msk.validateProperties();
        }
    }
}
