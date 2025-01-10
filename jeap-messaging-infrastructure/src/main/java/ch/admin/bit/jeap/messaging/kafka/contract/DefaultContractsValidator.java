package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


@Slf4j
public class DefaultContractsValidator implements ContractsValidator {

    private static final String ROLE_CONSUMER = "consumer";
    private static final String ROLE_PRODUCER = "producer";

    private final String appName;

    private final Map<String, List<Contract>> consumerContracts;
    private final Map<MessageTypeIdentificator, List<Contract>> producerContracts;

    public DefaultContractsValidator(@Value("${spring.application.name}") String appName, ContractsProvider contractsProvider) {
        this.appName = appName;

        this.consumerContracts = contractsProvider.getContracts().stream()
                .filter(c -> appName.equals(c.getAppName()))
                .filter(c -> c.getRole().equalsIgnoreCase(ROLE_CONSUMER))
                .collect(groupingBy(Contract::getMessageTypeName));

        this.producerContracts = contractsProvider.getContracts().stream()
                .filter(c -> appName.equals(c.getAppName()))
                .filter(c -> c.getRole().equalsIgnoreCase(ROLE_PRODUCER))
                .collect(groupingBy(MessageTypeIdentificator::from, toList()));
    }

    @Override
    public void ensurePublisherContract(MessageType messageType, String topic) {
        if (ContractValidationExemptions.isExemptedFromSenderValidation(messageType)) {
            return;
        }
        if (!hasProducerContractFor(messageType, topic)) {
            throw NoContractException.noContract(appName, ROLE_PRODUCER, messageType, topic);
        }
    }

    @Override
    public void ensureConsumerContract(String messageTypeName, String topic) {
        if (ContractValidationExemptions.isExemptedFromReceiverValidation(messageTypeName)) {
            return;
        }
        if (!hasConsumerContractFor(messageTypeName, topic)) {
            throw NoContractException.noContract(appName, ROLE_CONSUMER, messageTypeName, topic);
        }
    }

    private boolean hasProducerContractFor(MessageType messageType, String topic) {
        return producerContracts.getOrDefault(MessageTypeIdentificator.from(messageType), List.of()).stream()
                .flatMap(contract -> Arrays.stream(contract.getTopics()))
                .anyMatch(contractTopic -> contractTopic.equals(topic));
    }

    private boolean hasConsumerContractFor(String messageTypeName, String topic) {
        return consumerContracts.getOrDefault(messageTypeName, List.of()).stream()
                .flatMap(contract -> Arrays.stream(contract.getTopics()))
                .anyMatch(contractTopic -> contractTopic.equals(topic));
    }

}
