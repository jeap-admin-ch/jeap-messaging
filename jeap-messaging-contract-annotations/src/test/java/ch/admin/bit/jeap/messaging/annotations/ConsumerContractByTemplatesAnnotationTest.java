package ch.admin.bit.jeap.messaging.annotations;

import ch.admin.bit.jme.document.JmeDocumentReviewedEvent;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static ch.admin.bit.jeap.messaging.annotations.ConsumerContractByTemplatesAnnotationTest.APP_NAME;
import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.assertContractFileExists;
import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.loadContractJson;

/**
 * Maven: Maven configures annotation processors explicitly via the pom.xml file and ensures that all necessary paths and resources are available.
 * IntelliJ IDEA: IntelliJ IDEA may handle annotation processors differently, especially when they are configured through the IDE.
 * This can result in certain paths, such as StandardLocation.SOURCE_PATH, not being correctly set when the IDE manages the build and compile process.
 * Therefore, this integration test cannot be executed via the IDE (using the green arrows in the UI next to the tests) but must be run using the build tool.
 */
@JeapMessageConsumerContractsByTemplates(appName = APP_NAME)
public class ConsumerContractByTemplatesAnnotationTest {

    public static final String APP_NAME = "appNameForTest";
    public static final String EVENT_NAME = "JmeDocumentReviewedEvent";
    public static final String EVENT_VERSION = "1.0.0";

    /**
     * Tests if the generated File is there with the correct Name
     */
    @Test
    void generatedContractFileExists() {
        String expectedName = APP_NAME + "-" + EVENT_NAME + "-" + EVENT_VERSION + ".consumer-contract.json";
        assertContractFileExists(expectedName);
    }

    /**
     * Tests if the generated Json-File has the correct content
     */
    @Test
    void generatedContractJsonIsCorrect() throws Exception {
        String expectedName = APP_NAME + "-" + EVENT_NAME + "-" + EVENT_VERSION + ".consumer-contract.json";

        JSONObject actual = loadContractJson(expectedName);

        JSONAssert.assertEquals("{contractVersion:\"1.0.0\"}", actual, false);
        JSONAssert.assertEquals("{role:\"consumer\"}", actual, false);
        JSONAssert.assertEquals("{systemName:\"" + JmeDocumentReviewedEvent.TypeRef.SYSTEM_NAME + "\"}", actual, false);
        JSONAssert.assertEquals("{messageTypeName:\"" + JmeDocumentReviewedEvent.TypeRef.MESSAGE_TYPE_NAME + "\"}", actual, false);
        JSONAssert.assertEquals("{messageTypeVersion:\"" + JmeDocumentReviewedEvent.TypeRef.MESSAGE_TYPE_VERSION + "\"}", actual, false);
        JSONAssert.assertEquals("{registryUrl:\"" + JmeDocumentReviewedEvent.TypeRef.REGISTRY_URL + "\"}", actual, false);
        JSONAssert.assertEquals("{registryBranch:\"" + JmeDocumentReviewedEvent.TypeRef.REGISTRY_BRANCH + "\"}", actual, false);
        JSONAssert.assertEquals("{registryCommit:\"" + JmeDocumentReviewedEvent.TypeRef.REGISTRY_COMMIT + "\"}", actual, false);
        JSONAssert.assertEquals("{compatibilityMode:\"" + JmeDocumentReviewedEvent.TypeRef.COMPATIBILITY_MODE + "\"}", actual, false);
        JSONAssert.assertEquals("{appName:\"appNameForTest\"}", actual, false);
        JSONAssert.assertEquals("{topics: [ \"topic1\", \"topic2\" ]}", actual, false);
    }
}
