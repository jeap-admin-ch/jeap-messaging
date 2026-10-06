package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CompatibilityMode;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CreateMessageContractsDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractRole;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.contract.v2.SchemaResources;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MessageContracts {

    private static final String CONTRACT_SUFFIX = "-contract.json";
    private final boolean schemaUploadEnabled;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public MessageContracts() {
        this(true);
    }

    public MessageContracts(boolean schemaUploadEnabled) {
        this.schemaUploadEnabled = schemaUploadEnabled;
    }

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
            List<Contract> contracts = pathStream.map(this::deserializeContract).toList();
            Map<String, Set<String>> legacyIdentities = contracts.stream().collect(Collectors.groupingBy(
                    SchemaResources::legacyFileName,
                    Collectors.mapping(contract -> SchemaResources.fileName("", contract), Collectors.toSet())));
            Map<Path, Optional<String>> schemaCache = new HashMap<>();
            return contracts.stream()
                    .flatMap(contract -> toDtos(contract, readSchema(contractDirectory, contract, legacyIdentities, schemaCache)))
                    .collect(toList());
        }
    }

    private boolean isContractFile(Path path, BasicFileAttributes unused) {
        return path.toString().endsWith(CONTRACT_SUFFIX);
    }

    private Stream<MessageContractDto> toDtos(Contract contract, String schema) {
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
    private String readSchema(Path contractDirectory, Contract contract, Map<String, Set<String>> legacyIdentities,
                              Map<Path, Optional<String>> schemaCache) {
        if (!schemaUploadEnabled) {
            return null;
        }
        String resource = contract.getSchemaResource();
        if ("".equals(resource)) {
            return null; // Explicitly unavailable: do not pick up stale legacy output.
        }
        if (resource == null) {
            resource = SchemaResources.legacyFileName(contract);
            if (legacyIdentities.get(resource).size() != 1) {
                return null; // An old filename cannot distinguish these registries/systems.
            }
        }
        if (resource.contains("/") || resource.contains("\\") || !resource.endsWith(".avsc")) {
            throw new IllegalArgumentException("Invalid schema resource: " + resource);
        }
        Path schemaPath = contractDirectory.resolve(resource);
        return schemaCache.computeIfAbsent(schemaPath, path -> Optional.ofNullable(readSchemaFile(path))).orElse(null);
    }

    String readSchemaFile(Path schemaPath) {
        if (!Files.isRegularFile(schemaPath)) {
            return null;
        }
        try {
            return Files.readString(schemaPath);
        } catch (IOException ex) {
            throw MessageContractPublishException.failedToLoadSchema(schemaPath, ex);
        }
    }

    private Contract deserializeContract(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), Contract.class);
        } catch (JacksonException e) {
            throw MessageContractPublishException.contractDeserializationFailure(path, e);
        }
    }
}
