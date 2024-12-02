package ch.admin.bit.jeap.messaging.annotations.processor.appname;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppNameConfigReaderTest {

    /*
     * Note: App name detection from spring boot config files is also covered by the tests in jeap-messaging-contract-annotations
     */

    @Test
    void readSpringApplicationNameFromClasspathResource_singleLine() {
        Optional<String> singleLineAppName = AppNameConfigReader
                .findSpringApplicationNameInClasspathResource("app-name-single-line.yaml");

        assertEquals("app", singleLineAppName.orElseThrow());
    }

    @Test
    void readSpringApplicationNameFromClasspathResource_hierarchical() {
        Optional<String> hierarchicalAppName = AppNameConfigReader
                .findSpringApplicationNameInClasspathResource("app-name-hierarchical.yaml");

        assertEquals("app", hierarchicalAppName.orElseThrow());
    }

    @Test
    void readSpringApplicationNameFromClasspathResource_mixed() {
        Optional<String> mixedAppName1 = AppNameConfigReader
                .findSpringApplicationNameInClasspathResource("app-name-mixed.yaml");
        Optional<String> mixedAppName2 = AppNameConfigReader
                .findSpringApplicationNameInClasspathResource("app-name-mixed-2.yaml");

        assertEquals("app", mixedAppName1.orElseThrow());
        assertEquals("app", mixedAppName2.orElseThrow());
    }

    @Test
    void readSpringApplicationNameFromClasspathResource_invalidYaml() {
        Optional<String> invalidAppName = AppNameConfigReader
                .findSpringApplicationNameInClasspathResource("app-name-invalid.yaml");

        assertTrue(invalidAppName.isEmpty());
    }

    @Test
    void findSpringApplicationNames_fromYaml() throws IOException {
        Path workingDirectory = Paths.get("src", "test", "resources", "app");

        Set<String> appNames = AppNameConfigReader
                .findSpringApplicationNames(workingDirectory);

        assertThat(appNames)
                .containsExactly("app");
    }

    @Test
    void findSpringApplicationNames_fromProperties() throws IOException {
        Path workingDirectory = Paths.get("src", "test", "resources", "app-properties");

        Set<String> appNames = AppNameConfigReader
                .findSpringApplicationNames(workingDirectory);

        assertThat(appNames)
                .containsExactly("appBootstrapProperties");
    }

    @Test
    void findSpringApplicationNames_shouldFindMultipleAppNames() throws IOException {

        Path workingDirectory = Paths.get("src", "test", "resources", "app-ambiguous-app-names");

        Set<String> springApplicationNames = AppNameConfigReader.findSpringApplicationNames(workingDirectory);

        assertThat(springApplicationNames)
                .containsOnly("app1", "app2", "appProperties");
    }
}
