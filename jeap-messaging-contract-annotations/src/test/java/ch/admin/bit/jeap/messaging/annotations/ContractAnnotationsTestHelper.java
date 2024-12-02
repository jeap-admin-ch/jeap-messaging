package ch.admin.bit.jeap.messaging.annotations;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractAnnotationsTestHelper {

    private static final String CONTRACTS_FOLDER = "ch/admin/bit/jeap/messaging/contracts/";
    private static final String CONTRACTS_PATH = "target/test-classes/" + CONTRACTS_FOLDER;

    static void assertContractFileExists(String expectedName) {
        ClassLoader classLoader = ContractAnnotationsTestHelper.class.getClassLoader();
        URL url = classLoader.getResource(CONTRACTS_FOLDER + expectedName);
        assertNotNull(url);
        assertTrue(new File(url.getFile()).exists());
    }

    static JSONObject loadContractJson(String expectedName) throws IOException, JSONException {
        Path workingDir = Path.of(CONTRACTS_PATH);
        Path file = workingDir.resolve(expectedName);
        String content = Files.readString(file);
        return new JSONObject(content);
    }
}
