package ch.admin.bit.jeap.messaging.annotations;

import org.junit.jupiter.api.Test;

import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.loadSchema;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the avro schema is also written for a message type that is compiled in the same compilation run as the
 * contract annotation, i.e. where the class is not available as bytecode and the schema has to be read from the
 * SCHEMA$ field initializer in the source tree.
 */
@JeapMessageProducerContract(value = SameCompilationUnitTestEvent.TypeRef.class,
        appName = SameCompilationUnitSchemaTest.APP_NAME)
class SameCompilationUnitSchemaTest {

    public static final String APP_NAME = "appNameForSameCompilationUnitTest";

    @Test
    void generatedSchemaFileContainsTheSchemaOfTheMessageType() throws Exception {
        String expectedName = SameCompilationUnitTestEvent.TypeRef.MESSAGE_TYPE_NAME + "-" +
                              SameCompilationUnitTestEvent.TypeRef.MESSAGE_TYPE_VERSION + ".avsc";

        String schema = loadSchema(expectedName);

        assertEquals(SameCompilationUnitTestEvent.getClassSchema().toString(), schema);
    }
}
