package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class ProducerContractInterceptorTest {

    private static final String TOPIC = "test-topic";

    @Test
    void keepsRecordWhenContractExists() {
        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ProducerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, false);

        AvroMessage avroMessage = mock(AvroMessage.class);
        MessageType messageType = messageType("event-type", "1");
        when(avroMessage.getType()).thenReturn(messageType);
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(TOPIC, avroMessage);

        ProducerRecord<Object, Object> result = interceptor.onSend(producerRecord);

        assertSame(producerRecord, result);
        verify(contractsValidator).ensurePublisherContract(messageType, TOPIC);
    }

    @Test
    void returnsFailingRecordWhenNoContractAndNotAllowed(CapturedOutput output) {
        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ProducerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, false);

        ProducerRecord<Object, Object> producerRecord = recordWithoutContract(contractsValidator);

        ProducerRecord<Object, Object> result = interceptor.onSend(producerRecord);

        assertInstanceOf(NoContractProducerRecord.class, result);
        assertThat(output).contains("You have no contract to publish events");
    }

    @Test
    void keepsRecordWhenNoContractButAllowed(CapturedOutput output) {
        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ProducerContractInterceptor interceptor = configuredInterceptor(contractsValidator, true, false);

        ProducerRecord<Object, Object> producerRecord = recordWithoutContract(contractsValidator);

        ProducerRecord<Object, Object> result = interceptor.onSend(producerRecord);

        assertSame(producerRecord, result);
        assertThat(output).contains("You have no contract to publish events");
    }

    @Test
    void keepsRecordSilentlyWhenNoContractAndSilentAllowed(CapturedOutput output) {
        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ProducerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, true);

        ProducerRecord<Object, Object> producerRecord = recordWithoutContract(contractsValidator);

        ProducerRecord<Object, Object> result = interceptor.onSend(producerRecord);

        assertSame(producerRecord, result);
        verify(contractsValidator).ensurePublisherContract(any(), eq(TOPIC));
        assertThat(output).doesNotContain("You have no contract to publish events");
    }

    @Test
    void skipsValidationWhenProducerContractCheckIsExempt() {
        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ProducerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, false);

        AvroMessage avroMessage = mock(AvroMessage.class);
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(TOPIC, avroMessage);
        producerRecord.headers().add(ProducerContractInterceptor.EXEMPT_FROM_PRODUCER_CONTRACT_CHECK_HEADER, new byte[0]);

        ProducerRecord<Object, Object> result = interceptor.onSend(producerRecord);

        assertSame(producerRecord, result);
        verifyNoInteractions(contractsValidator);
    }

    @Test
    void skipsValidationForNonAvroMessages() {
        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ProducerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, false);

        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(TOPIC, "plain-value");

        ProducerRecord<Object, Object> result = interceptor.onSend(producerRecord);

        assertSame(producerRecord, result);
        verifyNoInteractions(contractsValidator);
    }

    private static ProducerRecord<Object, Object> recordWithoutContract(ContractsValidator contractsValidator) {
        AvroMessage avroMessage = mock(AvroMessage.class);
        MessageType messageType = messageType("event-type", "1");
        when(avroMessage.getType()).thenReturn(messageType);
        doThrow(NoContractException.noContract("test-app", "publisher", messageType.getName(), TOPIC))
                .when(contractsValidator).ensurePublisherContract(messageType, TOPIC);
        return new ProducerRecord<>(TOPIC, avroMessage);
    }

    private static ProducerContractInterceptor configuredInterceptor(ContractsValidator contractsValidator,
                                                                     boolean allowNoContractEvents,
                                                                     boolean allowNoContractEventsSilent) {
        ProducerContractInterceptor interceptor = new ProducerContractInterceptor();
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerContractInterceptor.CONTRACTS_VALIDATOR, contractsValidator);
        config.put(ProducerContractInterceptor.ALLOW_NO_CONTRACT_EVENTS, allowNoContractEvents);
        config.put(ProducerContractInterceptor.ALLOW_NO_CONTRACT_EVENTS_SILENT, allowNoContractEventsSilent);
        interceptor.configure(config);
        return interceptor;
    }

    private static MessageType messageType(String name, String version) {
        return new MessageType() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getVersion() {
                return version;
            }
        };
    }
}
