package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CompatibilityMode;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CreateMessageContractsDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractRole;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
    private static final String SCHEMA_SUFFIX = ".avsc";

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

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
        String schema = readSchema(path.getParent(), contract);
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
                        .schema(schema)
                        .build()
        );
    }

    /**
     * Reads the avro schema of the message type, which the annotation processor writes next to the contract file. If
     * no schema file is present, the message contract service reads the schema from the message type registry.
     */
    private String readSchema(Path contractDirectory, Contract contract) {
        Path schemaPath = contractDirectory.resolve(schemaFileName(contract));
        if (!Files.isRegularFile(schemaPath)) {
            return null;
        }
        try {
            return Files.readString(schemaPath);
        } catch (IOException ex) {
            throw MessageContractPublishException.failedToLoadSchema(schemaPath, ex);
        }
    }

    private String schemaFileName(Contract contract) {
        return contract.getMessageTypeName() + "-" + contract.getMessageTypeVersion() + SCHEMA_SUFFIX;
    }

    private Contract deserializeContract(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), Contract.class);
        } catch (JacksonException e) {
            throw MessageContractPublishException.contractDeserializationFailure(path, e);
        }
    }
}
