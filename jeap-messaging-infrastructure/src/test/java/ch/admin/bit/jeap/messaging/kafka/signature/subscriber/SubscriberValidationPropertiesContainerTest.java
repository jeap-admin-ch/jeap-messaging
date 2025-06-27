package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;


import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriberValidationPropertiesContainerTest {

    @Test
    void isSignatureRequired_returnTrue_whenRequiredAndNoExceptionConfigured() {
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(true, null, null, null, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertTrue(container.isSignatureRequired(""));
    }

    @Test
    void isSignatureRequired_returnTrue_whenRequiredAndExceptionForOtherMessageTypeConfigured() {
        String messageTypeName1 = "MyMessage1";
        String messageTypeName2 = "MyMessage2";

        Set<String> acceptUnsignedMessagetypeWhitelist = Set.of(messageTypeName1);
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(true, acceptUnsignedMessagetypeWhitelist, null, null, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertTrue(container.isSignatureRequired(messageTypeName2));
    }

    @Test
    void isSignatureRequired_returnFalse_whenRequiredAndExceptionForMessageTypeConfigured() {
        String messageTypeName1 = "MyMessage1";

        Set<String> acceptUnsignedMessagetypeWhitelist = Set.of(messageTypeName1);
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(true, acceptUnsignedMessagetypeWhitelist, null, null, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertFalse(container.isSignatureRequired(messageTypeName1));
    }


    @Test
    void isSignatureRequired_returnFalse_whenNotRequiredAndNoExceptionConfigured() {
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(false, null, null, null, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertFalse(container.isSignatureRequired(""));
    }

    @Test
    void isSignatureRequired_returnFalse_whenNotRequiredAndExceptionForOtherMessageTypeConfigured() {
        String messageTypeName1 = "MyMessage1";
        String messageTypeName2 = "MyMessage2";

        Set<String> acceptUnsignedMessagetypeWhitelist = Set.of(messageTypeName1);
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(false, acceptUnsignedMessagetypeWhitelist, null, null, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertFalse(container.isSignatureRequired(messageTypeName2));
    }

    @Test
    void isPublisherAllowedForMessage_returnTrue_whenServiceAndMessageConfigured() {
        String messageTypeName = "MyMessage";
        String serviceName = "MyService1";

        Map<String, List<String>> allowedPublishers = Map.of(messageTypeName, List.of(serviceName));
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(false, null, null, allowedPublishers, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertTrue(container.isPublisherAllowedForMessage(messageTypeName, serviceName));
    }

    @Test
    void isPublisherAllowedForMessage_returnTrue_whenNothingConfigured() {
        String messageTypeName = "MyMessage";
        String serviceName = "MyService";

        Map<String, List<String>> allowedPublishers = Map.of();
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(false, null, null, allowedPublishers, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        assertTrue(container.isPublisherAllowedForMessage(messageTypeName, serviceName));
    }

    @Test
    void isPublisherAllowedForMessage_returnFalse_whenOtherServiceConfigured() {
        String messageTypeName = "MyMessage";
        String serviceName1 = "MyService1";

        Map<String, List<String>> allowedPublishers = Map.of(messageTypeName, List.of(serviceName1));
        SignatureSubscriberProperties properties = new SignatureSubscriberProperties(false, null, null, allowedPublishers, null);
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(properties);
        container.init();

        String serviceName2 = "MyService2";
        assertFalse(container.isPublisherAllowedForMessage(messageTypeName, serviceName2));
    }
}
