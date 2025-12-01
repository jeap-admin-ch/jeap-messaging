package ch.admin.bit.jeap.messaging.kafka.serde.glue.naming;

import com.amazonaws.services.schemaregistry.common.AWSSchemaNamingStrategy;
import org.apache.avro.generic.GenericContainer;

public class GlueTopicRecordSchemaNamingStrategy implements AWSSchemaNamingStrategy {

    private static final String KEY_SUFFIX = "-key";

    @Override
    public String getSchemaName(String transportName, Object data) {
        return getSchemaName(transportName, data, false);
    }

    @Override
    public String getSchemaName(String transportName, Object data, boolean isKey) {
        return transportName + getSuffix(data, isKey);
    }

    @Override
    public String getSchemaName(String transportName) {
        return transportName;
    }

    private String getRecordName(GenericContainer gc) {
        return gc.getSchema().getFullName();
    }

    private String getSuffix(Object data, boolean isKey) {
        String typeSuffix;
        if (data instanceof GenericContainer gc) {
            typeSuffix = "-" + getRecordName(gc);
        } else {
            typeSuffix = "";
        }

        String useCaseSuffix = isKey ? KEY_SUFFIX : "";
        return typeSuffix + useCaseSuffix;
    }
}
