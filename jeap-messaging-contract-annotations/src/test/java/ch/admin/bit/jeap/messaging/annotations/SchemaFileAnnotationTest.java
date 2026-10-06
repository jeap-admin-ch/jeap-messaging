package ch.admin.bit.jeap.messaging.annotations;

import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import org.junit.jupiter.api.Test;

import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.countContractFiles;
import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.loadSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the avro schema of a message type is written as a file next to the message contract. The schema is read
 * from the SCHEMA$ field of the avro generated message type class, which is provided as an artifact in this case.
 */
@JeapMessageProducerContract(value = JmeSimpleTestEvent.TypeRef.class,
        appName = SchemaFileAnnotationTest.APP_NAME,
        topic = {"topic1"})
class SchemaFileAnnotationTest {

    public static final String APP_NAME = "appNameForSchemaTest";
    private static final String SCHEMA_FILE_NAME = JmeSimpleTestEvent.TypeRef.MESSAGE_TYPE_NAME + "-" +
                                                   JmeSimpleTestEvent.TypeRef.MESSAGE_TYPE_VERSION + ".avsc";

    @Test
    void generatedSchemaFileContainsTheSchemaOfTheMessageType() throws Exception {
        String schema = loadSchema(SCHEMA_FILE_NAME);

        assertEquals(JmeSimpleTestEvent.getClassSchema().toString(), schema);
    }

    /**
     * This message type is referenced by the producer contract of this test and by the consumer contract generated
     * from the process template in {@link ConsumerContractByTemplatesAnnotationTest}. The schema is written only once
     * for a message type version, no matter how many contracts reference it.
     */
    @Test
    void generatedSchemaFileExistsOnlyOncePerMessageTypeVersion() throws Exception {
        String messageTypeAndVersion = JmeSimpleTestEvent.TypeRef.MESSAGE_TYPE_NAME + "-" +
                                       JmeSimpleTestEvent.TypeRef.MESSAGE_TYPE_VERSION;

        assertEquals(2, countContractFiles("*" + messageTypeAndVersion + ".*-contract.json"));
        assertEquals(1, countContractFiles(ContractAnnotationsTestHelper.schemaResource(SCHEMA_FILE_NAME)));
    }
}
