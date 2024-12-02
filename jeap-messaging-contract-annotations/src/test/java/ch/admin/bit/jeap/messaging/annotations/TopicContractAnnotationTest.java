package ch.admin.bit.jeap.messaging.annotations;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.loadContractJson;

@JeapMessageConsumerContract(value = TypeRefExamples.TypeRef4.class, appName = TopicContractAnnotationTest.APP_NAME)
class TopicContractAnnotationTest {

    public static final String APP_NAME = "appNameForTest";

    /**
     * Tests if the generated Json-File has the default Topic-Value
     */
    @Test
    void generatedContractJsonIsCorrect() throws Exception {
        String expectedName = APP_NAME + "-" + TypeRefExamples.TypeRef4.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef4.MESSAGE_TYPE_VERSION + ".consumer-contract.json";

        JSONObject actual = loadContractJson(expectedName);

        JSONAssert.assertEquals("{topics: [ \"some-default-topic\" ]}", actual, false);
    }
}
