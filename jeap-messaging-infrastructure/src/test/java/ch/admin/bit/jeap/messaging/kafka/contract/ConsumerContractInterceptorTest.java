package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ConsumerContractInterceptorTest {

    @Test
    void filtersOutAvroRecordsWhenNoContractAndNotAllowed() {
        String topic = "test-topic";
        TopicPartition topicPartition = new TopicPartition(topic, 0);

        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ConsumerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, false, null);

        AvroMessage avroMessage = mock(AvroMessage.class);
        MessageType messageType = messageType("event-type", "1");
        when(avroMessage.getType()).thenReturn(messageType);
        doThrow(NoContractException.noContract("test-app", "consumer", messageType.getName(), topic))
                .when(contractsValidator).ensureConsumerContract(messageType, topic);

        ConsumerRecord<Object, Object> plainRecord = new ConsumerRecord<>(topic, 0, 0L, "k1", "plain-value");
        ConsumerRecord<Object, Object> avroRecord = new ConsumerRecord<>(topic, 0, 1L, "k2", avroMessage);
        ConsumerRecords<Object, Object> records = new ConsumerRecords<>(Map.of(topicPartition, List.of(plainRecord, avroRecord)), Map.of());

        ConsumerRecords<Object, Object> filtered = interceptor.onConsume(records);

        List<ConsumerRecord<Object, Object>> remainingRecords = filtered.records(topicPartition);
        assertEquals(1, remainingRecords.size());
        assertSame(plainRecord, remainingRecords.get(0));
        verify(contractsValidator).ensureConsumerContract(messageType, topic);
    }

    @Test
    void keepsAvroRecordsWhenNoContractButAllowed() {
        String topic = "test-topic";
        TopicPartition topicPartition = new TopicPartition(topic, 0);

        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ConsumerContractInterceptor interceptor = configuredInterceptor(contractsValidator, true, false, null);

        AvroMessage avroMessage = mock(AvroMessage.class);
        MessageType messageType = messageType("event-type", "1");
        when(avroMessage.getType()).thenReturn(messageType);
        doThrow(NoContractException.noContract("test-app", "consumer", messageType.getName(), topic))
                .when(contractsValidator).ensureConsumerContract(messageType, topic);

        ConsumerRecord<Object, Object> avroRecord = new ConsumerRecord<>(topic, 0, 0L, "k1", avroMessage);
        ConsumerRecords<Object, Object> records = new ConsumerRecords<>(Map.of(topicPartition, List.of(avroRecord)), Map.of());

        ConsumerRecords<Object, Object> filtered = interceptor.onConsume(records);

        List<ConsumerRecord<Object, Object>> remainingRecords = filtered.records(topicPartition);
        assertEquals(1, remainingRecords.size());
        assertSame(avroRecord, remainingRecords.get(0));
        verify(contractsValidator).ensureConsumerContract(messageType, topic);
    }

    @Test
    void skipsValidationWhenConsumerContractCheckIsExempt() {
        String topic = "test-topic";
        TopicPartition topicPartition = new TopicPartition(topic, 0);

        ContractsValidator contractsValidator = mock(ContractsValidator.class);
        ConsumerContractInterceptor interceptor = configuredInterceptor(contractsValidator, false, false, "true");

        AvroMessage avroMessage = mock(AvroMessage.class);
        ConsumerRecord<Object, Object> avroRecord = new ConsumerRecord<>(topic, 0, 0L, "k1", avroMessage);
        ConsumerRecords<Object, Object> records = new ConsumerRecords<>(Map.of(topicPartition, List.of(avroRecord)), Map.of());

        ConsumerRecords<Object, Object> result = interceptor.onConsume(records);

        List<ConsumerRecord<Object, Object>> remainingRecords = result.records(topicPartition);
        assertEquals(1, remainingRecords.size());
        assertSame(avroRecord, remainingRecords.get(0));
        verifyNoInteractions(contractsValidator);
    }

    private static ConsumerContractInterceptor configuredInterceptor(ContractsValidator contractsValidator,
                                                                     boolean allowNoContractEvents,
                                                                     boolean silentIgnoreWithoutContract,
                                                                     String exemptFromConsumerContractCheck) {
        ConsumerContractInterceptor interceptor = new ConsumerContractInterceptor();
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerContractInterceptor.CONTRACTS_VALIDATOR, contractsValidator);
        config.put(ConsumerContractInterceptor.ALLOW_NO_CONTRACT_EVENTS, allowNoContractEvents);
        config.put(ConsumerContractInterceptor.SILENT_IGNORE_NO_CONTRACT_EVENTS, silentIgnoreWithoutContract);
        if (exemptFromConsumerContractCheck != null) {
            config.put(ConsumerContractInterceptor.EXEMPT_FROM_CONSUMER_CONTRACT_CHECK, exemptFromConsumerContractCheck);
        }

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