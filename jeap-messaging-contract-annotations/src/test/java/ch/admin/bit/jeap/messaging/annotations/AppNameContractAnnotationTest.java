package ch.admin.bit.jeap.messaging.annotations;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.loadContractJson;

class AppNameContractAnnotationTest {

    public static final String APP_NAME = "appNameForTest";

    @Test
    @JeapMessageConsumerContract(value = TypeRefExamples.TypeRef5.class, appName = APP_NAME)
    void generatedContractWithAppNameFromAnnotation() throws Exception {
        String expectedName = APP_NAME + "-" + TypeRefExamples.TypeRef5.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef5.MESSAGE_TYPE_VERSION + ".consumer-contract.json";

        JSONObject actual = loadContractJson(expectedName);

        JSONAssert.assertEquals("{appName: \"appNameForTest\"}", actual, false);
    }

    @Test
    @JeapMessageConsumerContract(TypeRefExamples.TypeRef6.class)
    void generatedContractWithAppNameFromApplicaitonYaml() throws Exception {
        String expectedName = "application-name-from-application-yaml-" + TypeRefExamples.TypeRef6.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef6.MESSAGE_TYPE_VERSION + ".consumer-contract.json";

        JSONObject actual = loadContractJson(expectedName);

        JSONAssert.assertEquals("{appName: \"application-name-from-application-yaml\"}", actual, false);
    }
}
