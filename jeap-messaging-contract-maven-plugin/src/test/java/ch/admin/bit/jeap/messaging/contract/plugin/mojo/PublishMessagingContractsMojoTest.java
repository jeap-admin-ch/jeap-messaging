package ch.admin.bit.jeap.messaging.contract.plugin.mojo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SuppressWarnings("java:S1874")
public class PublishMessagingContractsMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();
    @Rule
    public WireMockRule wireMock = new WireMockRule(options().port(12944));

    private static final String RESOURCE_PATH = "/api/contracts/test-app/1.0.0-SNAPSHOT";

    @Test
    public void execute() throws Exception {
        wireMock.stubFor(put(RESOURCE_PATH).willReturn(created()));

        File testDirectory = new File("src/test/resources/mojoTest");
        final PublishMessagingContractsMojo mojo = (PublishMessagingContractsMojo)
                mojoRule.lookupConfiguredMojo(testDirectory, "publish-messaging-contracts");

        mojo.execute();

        wireMock.verify(putRequestedFor(urlEqualTo(RESOURCE_PATH))
                .withRequestBody(
                        matchingJsonPath("$.contracts.length()", equalTo("1"))));
    }

    @Test(expected = MojoFailureException.class)
    public void execute_noCredentials_shouldFail() throws Exception {
        File testDirectory = new File("src/test/resources/mojoTestNoCredentials");
        final PublishMessagingContractsMojo mojo = (PublishMessagingContractsMojo)
                mojoRule.lookupConfiguredMojo(testDirectory, "publish-messaging-contracts");

        mojo.execute();
    }
}
