package ch.admin.bit.jeap.messaging.kafka.log;

import ch.admin.bit.jeap.messaging.model.MessageType;
import ch.admin.bit.jeap.messaging.model.MessageUser;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageLoggerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Test
    void writeTo_whenMessageWithoutVariantAndUser_shouldWriteBasicFields() throws IOException {
        TestEvent testEvent = new TestEvent();
        MessageLogger messageLogger = MessageLogger.message(testEvent);

        messageLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringField("messageType", "TestEvent");
        verify(jsonGenerator).writeStringField("messageVersion", "1.0.0");
        verify(jsonGenerator).writeStringField("messageId", "id");
        verify(jsonGenerator).writeStringField("messageIdempotenceId", "idempotence-id");
        verify(jsonGenerator).writeStringField("messagePublisherSystem", "system");
        verify(jsonGenerator).writeStringField("messagePublisherService", "system-service");
        verify(jsonGenerator).writeStringField("messageUserId", null);
        verify(jsonGenerator).writeStringField(eq("messageCreated"), anyString());
        verify(jsonGenerator, never()).writeStringField(eq("messageVariant"), anyString());
    }

    @Test
    void writeTo_whenMessageWithVariant_shouldWriteVariantField() throws IOException {
        TestEventWithVariant testEvent = new TestEventWithVariant();
        MessageLogger messageLogger = MessageLogger.message(testEvent);

        messageLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringField("messageVariant", "test-variant");
        verify(jsonGenerator).writeStringField("messageType", "TestEventWithVariant");
    }

    @Test
    void writeTo_whenMessageWithUser_shouldWriteUserId() throws IOException {
        TestEventWithUser testEvent = new TestEventWithUser();
        MessageLogger messageLogger = MessageLogger.message(testEvent);

        messageLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringField("messageUserId", "user-123");
    }

    @Test
    void writeTo_whenSimpleMessageLogger_shouldWriteOnlyMessageType() throws IOException {
        MessageLogger simpleLogger = MessageLogger.message("test string");

        simpleLogger.writeTo(jsonGenerator);

        verify(jsonGenerator).writeStringField("messageType", "String");
        verify(jsonGenerator, times(1)).writeStringField(anyString(), anyString());
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
