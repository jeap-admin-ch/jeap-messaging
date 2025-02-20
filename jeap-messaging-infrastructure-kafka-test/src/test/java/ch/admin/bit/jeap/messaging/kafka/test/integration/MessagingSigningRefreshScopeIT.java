package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureConfiguration;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.SubscriberCertificatesContainer;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.SubscriberValidationPropertiesContainer;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@AutoConfigureObservability
@SpringBootTest(classes = {TestConfig.class, SignatureConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=jme-messaging-receiverpublisher-service",
                "management.endpoint.prometheus.access=unrestricted",
                "management.endpoints.web.exposure.include=*"
        })
@ActiveProfiles({"test-signing-publisher", "test-signing-subscriber"})
public class MessagingSigningRefreshScopeIT extends KafkaIntegrationTestBase {

    private static final String NEW_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEcDCCAlgCFB0smObKeFzDo5It53+YnZ1OppZNMA0GCSqGSIb3DQEBCwUAMG8x\n" +
            "CzAJBgNVBAYTAkNIMQ0wCwYDVQQIDARCZXJuMQ0wCwYDVQQHDARCZXJuMRswGQYD\n" +
            "VQQKDBJUZXN0SW50ZXJtZWRpYXRlQ0ExDDAKBgNVBAsMA0JJVDEXMBUGA1UEAwwO\n" +
            "SW50ZXJtZWRpYXRlQ0EwHhcNMjUwMjI0MTU0MDEzWhcNMzMwNTEzMTU0MDEzWjB6\n" +
            "MQswCQYDVQQGEwJDSDENMAsGA1UECAwEQmVybjENMAsGA1UEBwwEQmVybjEMMAoG\n" +
            "A1UECgwDQklUMQ0wCwYDVQQLDARqZWFwMTAwLgYDVQQDDCdqbWUtbWVzc2FnaW5n\n" +
            "LXJlY2VpdmVycHVibGlzaGVyLXNlcnZpY2UwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
            "DwAwggEKAoIBAQDSLRtMrkpadLeYFPw2KhQPhwnpe7u3jSq2YIM2aC8LyhYRHey+\n" +
            "ex6umErMJ19on4YGQkLp/We4Sro36b8LM//uQVVCAIAxvQWjlo9cNXER2hflBz0N\n" +
            "m368ijKblCYPiH5dGZixZdS2RBcqpzGYeC0eNO1pVPK8xFwrVxKnZOBWhhZ6tz25\n" +
            "ogoQ1T6ENb57ql1Ki2xxpeS0bVYBYAFx3v2Q+iT+FF1kL7LM1JVOBI/0y+JlUEFL\n" +
            "7ilKUgmZ/nihibj7P8wb6jQ5ZSaUK1YN5s8noZwyPGFacau6ACGpVee5xEGc7hDo\n" +
            "rqT9jep/yS3usb3Q17dESrpwNB1t2RQ+o3e3AgMBAAEwDQYJKoZIhvcNAQELBQAD\n" +
            "ggIBAJHbIhXFJXCJg18l6fyfHonZTguYck+zPYo88f/5UKbjhBWOkbJR/Yohq0rc\n" +
            "qII0bTTO8/jHZvwlwwwfk4LmDn2Q41i5AVHNF0aGuEyMfhewI2jb+OLyRlupJ8e0\n" +
            "Jzvk2nOkKwN1f+hJ06DEuj9de2iiWqYbFreNGbQHaXGRgthEMz//uRdsA6aFyqWV\n" +
            "Yeji4qxiJnxD+uHlokKiHhH8RGmq5xf+N9yS5yRF+paMvgJU/4cz/tH/GPwifLWb\n" +
            "6eAPu6alsB0s7vqMBwvZph8xzhtYdpH8naIoMpn+o9QaWQpPSlxQgDAqohlecBux\n" +
            "7lYY0PEQTIxZ6AMJZkdUmibsXNqLEqYPuGmWkH1h1VNQQaNuQDv/Hs4HAkFZe5oL\n" +
            "bJmJzOxckCzTWwbHSRnS+1CAhteoFFPG6TPd3t+1I487SMkC1X1ZLi3Tuz1JKL7s\n" +
            "8Npc11FsdIrCDmGrAu4YAfk1nAHe/SUpqmIlFksNm/JFRVhA7O8O/Fi5tF71lcT0\n" +
            "YMUcgZetB9sqfG9Y/vOnWjONF31QTpoM32EwbOTqlea43IqkRdIeK7EHwpwOivd9\n" +
            "2DkftLqrCjVMlN2aZQieQH+AxqDaDNC4puRMDE6IHf+TsuAs3MbQJ1p3RARvDuM6\n" +
            "Si9uZaeRY6nmo6HOTcpCN6RUMqRi2g9/WEHn3qPSYWgjlRcF\n" +
            "-----END CERTIFICATE-----\n";


    @Autowired
    private ContextRefresher refresher;

    @Autowired
    private ConfigurableApplicationContext configurableApplicationContext;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SubscriberValidationPropertiesContainer subscriberValidationPropertiesContainer;

    @Autowired
    private SubscriberCertificatesContainer subscriberCertificatesContainer;

    @Test
    void testRefreshScope_updateCertificateChain() {
        byte[] newCertificateSerialNumber = HexFormat.ofDelimiter(" ").parseHex("1D 2C 98 E6 CA 78 5C C3 A3 92 2D E7 7F 98 9D 9D 4E A6 96 4D");
        assertNull(subscriberCertificatesContainer.getCertificateWithSerialNumber(newCertificateSerialNumber));
        TestPropertyValues.of(
                "jeap.messaging.authentication.subscriber.certificate-chains.jme-messaging-receiverpublisher-service[0].chain[0]= |-\n" + NEW_CERTIFICATE
        ).applyTo(configurableApplicationContext);

        refresher.refresh();

        assertNotNull(subscriberCertificatesContainer.getCertificateWithSerialNumber(newCertificateSerialNumber));
    }

    @Test
    void testRefreshScope_updateAllowedPublishers() {
        String newAllowedService = "new-service";
        assertFalse(subscriberValidationPropertiesContainer.isPublisherAllowedForMessage("JmeCreateDeclarationCommand", newAllowedService));
        TestPropertyValues.of(
                "jeap.messaging.authentication.subscriber.allowed-publishers.JmeCreateDeclarationCommand=jme-messaging-receiverpublisher-service," + newAllowedService
        ).applyTo(configurableApplicationContext);

        refresher.refresh();

        assertTrue(subscriberValidationPropertiesContainer.isPublisherAllowedForMessage("JmeCreateDeclarationCommand", newAllowedService));
    }

    @Test
    void testRefreshScope_updateMessageTypeWhiteList() {
        String newMessageTypeName = "NewMessageType";
        assertTrue(subscriberValidationPropertiesContainer.isSignatureRequired(newMessageTypeName));
        TestPropertyValues.of(
                "jeap.messaging.authentication.subscriber.accept-unsigned-messagetype-whitelist=" + newMessageTypeName
        ).applyTo(configurableApplicationContext);

        refresher.refresh();

        assertFalse(subscriberValidationPropertiesContainer.isSignatureRequired(newMessageTypeName));
    }
}
