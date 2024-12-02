package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CreateMessageContractsDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageContractPublisher {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MessageContractServiceClient messageContractServiceClient;
    private final Log log;

    public MessageContractPublisher(MessageContractServiceClient messageContractServiceClient, Log log) {
        this.messageContractServiceClient = messageContractServiceClient;
        this.log = log;
    }

    public void publishMessageContracts(Path contractPath, String appVersion, String transactionId) {
        MessageContracts messageContracts = new MessageContracts();
        Optional<CreateMessageContractsDto> maybeDto = messageContracts.loadAll(contractPath);
        maybeDto.ifPresent(dto -> publishMessageContracts(dto, appVersion, transactionId));
    }

    private void publishMessageContracts(CreateMessageContractsDto createMessageContractsDto, String appVersion, String transactionId) {
        String appName = appName(createMessageContractsDto.getContracts());
        String json = toJson(createMessageContractsDto);
        log.info("Publishing " + createMessageContractsDto.getContracts().size() + " messaging contracts");
        if (log.isDebugEnabled()) {
            log.debug("Publishing messaging contracts JSON: " + json);
        }
        messageContractServiceClient.putContracts(appName, appVersion, json, transactionId);
    }

    private String appName(List<MessageContractDto> contracts) {
        Set<String> appNames = contracts.stream()
                .map(MessageContractDto::getAppName)
                .collect(Collectors.toSet());
        if (appNames.size() > 1) {
            throw MessageContractPublishException.ambiguousAppNames(appNames);
        }
        String appName = appNames.iterator().next();
        if (appName == null || appName.isBlank()) {
            throw MessageContractPublishException.badAppName(appName);
        }
        return appName;
    }

    private String toJson(CreateMessageContractsDto createMessageContractsDto) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(createMessageContractsDto);
        } catch (Exception e) {
            throw MessageContractPublishException.jsonSerializationFailure(e);
        }
    }
}
