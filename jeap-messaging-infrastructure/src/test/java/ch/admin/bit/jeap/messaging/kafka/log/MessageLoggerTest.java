package ch.admin.bit.jeap.messaging.kafka.log;

import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageLoggerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Test
    void writeTo_whenMessageWithoutVariantAndUser_shouldWriteBasicFields() {
        TestEvent testEvent = new TestEvent();
        MessageLogger messageLogger = MessageLogger.message(testEvent);

        messageLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringProperty("messageType", "TestEvent");
        verify(jsonGenerator).writeStringProperty("messageVersion", "1.0.0");
        verify(jsonGenerator).writeStringProperty("messageId", "id");
        verify(jsonGenerator).writeStringProperty("messageIdempotenceId", "idempotence-id");
        verify(jsonGenerator).writeStringProperty("messagePublisherSystem", "system");
        verify(jsonGenerator).writeStringProperty("messagePublisherService", "system-service");
        verify(jsonGenerator).writeStringProperty("messageUserId", null);
        verify(jsonGenerator).writeStringProperty(eq("messageCreated"), anyString());
        verify(jsonGenerator, never()).writeStringProperty(eq("messageVariant"), anyString());
    }

    @Test
    void writeTo_whenMessageWithVariant_shouldWriteVariantField() {
        TestEventWithVariant testEvent = new TestEventWithVariant();
        MessageLogger messageLogger = MessageLogger.message(testEvent);

        messageLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringProperty("messageVariant", "test-variant");
        verify(jsonGenerator).writeStringProperty("messageType", "TestEventWithVariant");
    }

    @Test
    void writeTo_whenMessageWithUser_shouldWriteUserId() {
        TestEventWithUser testEvent = new TestEventWithUser();
        MessageLogger messageLogger = MessageLogger.message(testEvent);

        messageLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringProperty("messageUserId", "user-123");
    }

    @Test
    void writeTo_whenSimpleMessageLogger_shouldWriteOnlyMessageType() {
        MessageLogger simpleLogger = MessageLogger.message("test string");

        simpleLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringProperty("messageType", "String");
        verify(jsonGenerator, times(1)).writeStringProperty(anyString(), anyString());
    }

    @Test
    void toString_whenSimpleMessageLogger_shouldReturnMessageType() {
        MessageLogger simpleLogger = MessageLogger.message("test string");

        String result = simpleLogger.toString();

        assertThat(result).isEqualTo("String");
    }

    @Test
    void toString_whenCustomObject_shouldReturnClassName() {
        CustomTestObject customObject = new CustomTestObject();
        MessageLogger simpleLogger = MessageLogger.message(customObject);

        String result = simpleLogger.toString();

        assertThat(result).isEqualTo("CustomTestObject");
    }

    // Test classes for different scenarios
    private static class TestEventWithVariant extends TestEvent {
        @Override
        public MessageType getType() {
            return new MessageType() {
                @Override
                public String getName() {
                    return "TestEventWithVariant";
                }

                @Override
                public String getVersion() {
                    return "1.0.0";
                }

                @Override
                public String getVariant() {
                    return "test-variant";
                }
            };
        }
    }

    private static class TestEventWithUser extends TestEvent {
        @Override
        public Optional<? extends MessageUser> getOptionalUser() {
            return Optional.of(new MessageUser() {
                @Override
                public String getId() {
                    return "user-123";
                }

                @Override
                public String getGivenName() {
                    return "John";
                }

                @Override
                public String getFamilyName() {
                    return "Doe";
                }

                @Override
                public String getBusinessPartnerName() {
                    return "Business Partner";
                }

                @Override
                public String getBusinessPartnerId() {
                    return "bp-123";
                }

                @Override
                public java.util.Map<String, String> getPropertiesMap() {
                    return java.util.Map.of();
                }
            });
        }
    }

    private static class CustomTestObject {
        // Empty class for testing class name extraction
    }
}
