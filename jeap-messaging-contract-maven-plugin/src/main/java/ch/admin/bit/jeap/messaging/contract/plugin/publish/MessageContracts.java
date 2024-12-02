package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CompatibilityMode;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CreateMessageContractsDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractRole;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MessageContracts {

    private static final String CONTRACT_SUFFIX = "-contract.json";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Optional<CreateMessageContractsDto> loadAll(Path contractPath) {
        if (Files.exists(contractPath)) {
            if (Files.isDirectory(contractPath)) {
                return loadContractsInDirectory(contractPath);
            } else {
                throw MessageContractPublishException.contractPathIsNotADirectory(contractPath);
            }
        }
        return Optional.empty();
    }

    private Optional<CreateMessageContractsDto> loadContractsInDirectory(Path contractDirectory) {
        List<MessageContractDto> contracts = contracts(contractDirectory);
        return contracts.isEmpty() ? Optional.empty() : Optional.of(new CreateMessageContractsDto(contracts));
    }

    private List<MessageContractDto> contracts(Path contractDirectory) {
        try {
            return loadContracts(contractDirectory);
        } catch (Exception ex) {
            throw MessageContractPublishException.failedToLoadContracts(contractDirectory, ex);
        }
    }

    private List<MessageContractDto> loadContracts(Path contractDirectory) throws IOException {
        try (Stream<Path> pathStream = Files.find(contractDirectory, 1, this::isContractFile)) {
            return pathStream
                    .flatMap(this::toDtos)
                    .collect(toList());
        }
    }

    private boolean isContractFile(Path path, BasicFileAttributes unused) {
        return path.toString().endsWith(CONTRACT_SUFFIX);
    }

    private Stream<MessageContractDto> toDtos(Path path) {
        Contract contract = deserializeContract(path);
        Stream<String> topicStream = Arrays.stream(contract.getTopics());
        return topicStream.map(topic ->
                MessageContractDto.builder()
                        .appName(contract.getAppName())
                        .messageType(contract.getMessageTypeName())
                        .messageTypeVersion(contract.getMessageTypeVersion())
                        .topic(topic)
                        .role(MessageContractRole.fromString(contract.getRole()))
                        .compatibilityMode(CompatibilityMode.fromString(contract.getCompatibilityMode()))
                        .registryUrl(contract.getRegistryUrl())
                        .branch(contract.getRegistryBranch())
                        .commitHash(contract.getRegistryCommit())
                        .encryptionKeyId(contract.getEncryptionKeyId())
                        .build()
        );
    }

    private Contract deserializeContract(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), Contract.class);
        } catch (IOException e) {
            throw MessageContractPublishException.contractDeserializationFailure(path, e);
        }
    }
}
