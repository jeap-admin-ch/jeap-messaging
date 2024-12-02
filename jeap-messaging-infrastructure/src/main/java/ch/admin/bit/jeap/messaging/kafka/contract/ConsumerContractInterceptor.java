package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ConsumerContractInterceptor implements ConsumerInterceptor<Object, Object> {
    public static final String CONTRACTS_VALIDATOR = "consumerContractInterceptor.contractsValidator";
    public static final String ALLOW_NO_CONTRACT_EVENTS = "consumerContractInterceptor.allowNoContractEvents";
    public static final String SILENT_IGNORE_NO_CONTRACT_EVENTS = "consumerContractInterceptor.silentIgnoreWithoutContract";
    public static final String EXEMPT_FROM_CONSUMER_CONTRACT_CHECK = "consumerContractInterceptor.exemptFromConsumerContractCheck";

    private ContractsValidator contractsValidator;
    private boolean allowNoContractEvents;
    private boolean silentIgnoreWithoutContract;

    @Override
    public void configure(Map<String, ?> configs) {
        contractsValidator = (ContractsValidator) configs.get(CONTRACTS_VALIDATOR);
        allowNoContractEvents = (Boolean) configs.get(ALLOW_NO_CONTRACT_EVENTS);
        silentIgnoreWithoutContract = (Boolean) configs.get(SILENT_IGNORE_NO_CONTRACT_EVENTS);
        boolean exemptFromConsumerContractCheck = isExemptFromConsumerContractCheck(configs);
        if (exemptFromConsumerContractCheck) {
            silentIgnoreWithoutContract = true;
            allowNoContractEvents = true;
        }
    }

    private static boolean isExemptFromConsumerContractCheck(Map<String, ?> configs) {
        Object exemptPropertyValue = configs.get(EXEMPT_FROM_CONSUMER_CONTRACT_CHECK);
        return exemptPropertyValue != null && "true".equals(exemptPropertyValue.toString());
    }

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        if (silentIgnoreWithoutContract) {
            return records;
        }

        Map<TopicPartition, List<ConsumerRecord<Object, Object>>> filtered = records.partitions().stream()
                .collect(Collectors.toMap(Function.identity(), partition -> onConsumeTopicPartition(records.records(partition))));
        return new ConsumerRecords<>(filtered);
    }

    private List<ConsumerRecord<Object, Object>> onConsumeTopicPartition(List<ConsumerRecord<Object, Object>> input) {
        return input.stream()
                .filter(this::isConsummationAllowed)
                .toList();
    }

    private boolean isConsummationAllowed(ConsumerRecord<Object, Object> record) {
        if (!(record.value() instanceof AvroMessage avroMessage)) {
            return true;
        }

        MessageType type = avroMessage.getType();
        String topic = record.topic();
        try {
            contractsValidator.ensureConsumerContract(type, topic);
            return true;
        } catch (NoContractException e) {
            if (allowNoContractEvents) {
                String message = String.format("You have no contract to consume events of type %s from topic %s. " +
                                "However as consumeWithoutContractAllowed is ON this event will still be consumed. " +
                                "If you do not want to see this message set silentIgnoreWithoutContract to true.",
                        type, topic);
                log.warn(message);
            } else {
                String message = String.format("You have no contract to consume events of type %s from topic %s. " +
                                "This event is filtered out and will not be consumed by this application. " +
                                "Use consumeWithoutContractAllowed to change this behavior in a dev environment. " +
                                "If you do not want to see this message set silentIgnoreWithoutContract to true.",
                        type, topic);
                log.warn(message, e);
            }
            return allowNoContractEvents;
        }
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // Nothing to do here. The contract checks are triggered by the consumption of messages.
    }

    @Override
    public void close() {
        // Nothing to clean-up on close.
    }
}
