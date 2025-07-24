package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.auth.aws.iam.AwsRolesAnywhereSessionOrchestrator;
import ch.admin.bit.jeap.messaging.auth.aws.iam.models.AwsRolesAnywhereSessionsResponse;
import ch.admin.bit.jeap.messaging.auth.aws.iam.models.CredentialSet;
import ch.admin.bit.jeap.messaging.auth.aws.iam.models.Credentials;
import ch.admin.bit.jeap.messaging.auth.aws.iam.properties.RolesAnywhereAutoConfiguration;
import ch.admin.bit.jeap.messaging.kafka.auth.aws.iam.test.TestApp;
import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.msk.auth.iam.IAMLoginModule;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = {
                TestApp.class,
                RolesAnywhereAutoConfiguration.class,
                AwsMskKafkaAuthPropertiesRolesAnywhereTest.MockConfig.class
        },
        properties = {
                "spring.application.name=testapp",
                "spring.main.allow-bean-definition-overriding=true",
                "jeap.aws.rolesanywhere.enabled=true",
                "jeap.messaging.kafka.cluster.test-cluster.aws.msk.iam-auth-enabled=true",
                "jeap.messaging.kafka.cluster.test-cluster.aws.msk.region=eu-test-1"
        }
)
@SetSystemProperty(key = "javax.net.ssl.trustStore", value = "teststore")
@SetSystemProperty(key = "javax.net.ssl.trustStorePassword", value = "testsecret")
class AwsMskKafkaAuthPropertiesRolesAnywhereTest {

    public static final String MOCK_ACCESS_KEY = "mockAccessKey";
    public static final String MOCK_SECRET_KEY = "mockSecretKey";
    public static final String EXPIRATION = "2025-12-31T23:59:59Z";

    @TestConfiguration
    static class MockConfig {

        @Bean
        public AwsRolesAnywhereSessionOrchestrator awsRolesAnywhereSessionOrchestrator() {
            AwsRolesAnywhereSessionOrchestrator mock = Mockito.mock(AwsRolesAnywhereSessionOrchestrator.class);

            var credentials = new Credentials();
            credentials.setAccessKeyId(MOCK_ACCESS_KEY);
            credentials.setSecretAccessKey(MOCK_SECRET_KEY);
            credentials.setSessionToken("mockSessionToken");
            credentials.setExpiration(EXPIRATION);

            var credentialSet = new CredentialSet();
            credentialSet.setCredentials(credentials);

            var response = new AwsRolesAnywhereSessionsResponse();
            response.setCredentialSet(List.of(credentialSet));

            when(mock.getIamRolesAnywhereSessions()).thenReturn(response);

            return mock;
        }
    }

    @Autowired
    private AwsMskKafkaAuthProperties kafkaAuthProperties;

    @Test
    void authenticationProperties_withRolesAnywhere() {
        Map<String, Object> props = kafkaAuthProperties.authenticationProperties("test-cluster");

        assertThat(props)
                .containsEntry(SaslConfigs.SASL_MECHANISM, "AWS_MSK_IAM")
                .containsEntry(SaslConfigs.SASL_JAAS_CONFIG, IAMLoginModule.class.getName() + " required;")
                .containsEntry(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, IAMRolesAnywhereCallbackHandler.class.getName());
    }
}
