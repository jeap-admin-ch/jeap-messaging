package ch.admin.bit.jeap.messaging.contract.plugin.mojo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MojoTest
class PublishMessagingContractsMojoTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(12944))
            .build();

    @Inject
    private MavenProject project;

    private static final String RESOURCE_PATH = "/api/contracts/test-app/1.0.0-SNAPSHOT";

    @Test
    @Basedir("src/test/resources/mojoTest")
    @InjectMojo(goal = "publish-messaging-contracts")
    void execute(PublishMessagingContractsMojo mojo) throws Exception {
        wireMock.stubFor(put(RESOURCE_PATH).willReturn(created()));

        // Set project properties that were in the test POM's <properties> section
        project.getProperties().setProperty("MESSAGE_CONTRACT_SERVICE_USERNAME", "user");
        project.getProperties().setProperty("MESSAGE_CONTRACT_SERVICE_PASSWORD", "secret");
        project.setVersion("1.0.0-SNAPSHOT");
        project.setArtifactId("test-app");

        mojo.execute();

        wireMock.verify(putRequestedFor(urlEqualTo(RESOURCE_PATH))
                .withRequestBody(
                        matchingJsonPath("$.contracts.length()", equalTo("1"))));
    }

    @Test
    @Basedir("src/test/resources/mojoTestNoCredentials")
    @InjectMojo(goal = "publish-messaging-contracts")
    void execute_noCredentials_shouldFail(PublishMessagingContractsMojo mojo) {
        assertThrows(MojoFailureException.class, mojo::execute);
    }
}
