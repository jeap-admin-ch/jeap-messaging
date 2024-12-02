package ch.admin.bit.jeap.messaging.annotations;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.assertContractFileExists;
import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.loadContractJson;

@JeapMessageProducerContract(value = TypeRefExamples.TypeRef1.class,
        appName = SingleProducerContractAnnotationTest.APP_NAME,
                             topic = {"topic1", "topic2"},
                             encryptionKeyId = "some-key-id")
class SingleProducerContractAnnotationTest {

    public static final String APP_NAME = "appNameForTest";

    /**
     * Tests if the generated File is there with the correct Name
     */
    @Test
    void generatedContractFileExists() {
        String expectedName = APP_NAME + "-" + TypeRefExamples.TypeRef1.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef1.MESSAGE_TYPE_VERSION + ".producer-contract.json";
        assertContractFileExists(expectedName);
    }

    /**
     * Tests if the generated Json-File has the correct content
     */
    @Test
    void generatedContractJsonIsCorrect() throws Exception {
        String expectedName = APP_NAME + "-" + TypeRefExamples.TypeRef1.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef1.MESSAGE_TYPE_VERSION + ".producer-contract.json";

        JSONObject actual = loadContractJson(expectedName);

        JSONAssert.assertEquals("{contractVersion:\"1.0.0\"}", actual, false);
        JSONAssert.assertEquals("{role:\"producer\"}", actual, false);
        JSONAssert.assertEquals("{systemName:\"" + TypeRefExamples.TypeRef1.SYSTEM_NAME + "\"}", actual, false);
        JSONAssert.assertEquals("{messageTypeName:\"" + TypeRefExamples.TypeRef1.MESSAGE_TYPE_NAME + "\"}", actual, false);
        JSONAssert.assertEquals("{messageTypeVersion:\"" + TypeRefExamples.TypeRef1.MESSAGE_TYPE_VERSION + "\"}", actual, false);
        JSONAssert.assertEquals("{registryUrl:\"" + TypeRefExamples.TypeRef1.REGISTRY_URL + "\"}", actual, false);
        JSONAssert.assertEquals("{registryBranch:\"" + TypeRefExamples.TypeRef1.REGISTRY_BRANCH + "\"}", actual, false);
        JSONAssert.assertEquals("{registryCommit:\"" + TypeRefExamples.TypeRef1.REGISTRY_COMMIT + "\"}", actual, false);
        JSONAssert.assertEquals("{compatibilityMode:\"" + TypeRefExamples.TypeRef1.COMPATIBILITY_MODE + "\"}", actual, false);
        JSONAssert.assertEquals("{appName:\"appNameForTest\"}", actual, false);
        JSONAssert.assertEquals("{topics: [ \"topic1\", \"topic2\" ]}", actual, false);
        JSONAssert.assertEquals("{encryptionKeyId:\"some-key-id\"}", actual, false);
    }

}
