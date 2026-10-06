package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CreateMessageContractsDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MessageContractsTest {

    private static final Path VALID_CONTRACTS = Path.of("src/test/resources/validContracts");
    private static final String MESSAGE_TYPE = "JmeCreateDeclarationCommand";

    private final MessageContracts messageContracts = new MessageContracts();

    @TempDir
    Path directory;

    @Test
    void explicitReferencesSeparateCollidingNamesAndDisabledUploadIgnoresAllFiles() throws Exception {
        writeContract("one", "first.avsc", "registry-one");
        writeContract("two", "second.avsc", "registry-two");
        Files.writeString(directory.resolve("first.avsc"), "first schema");
        Files.writeString(directory.resolve("second.avsc"), "second schema");
        Files.writeString(directory.resolve("Event-1.avsc"), "stale legacy schema");

        assertThat(messageContracts.loadAll(directory).orElseThrow().getContracts())
                .extracting(MessageContractDto::getSchema).containsExactlyInAnyOrder("first schema", "second schema");
        assertThat(new MessageContracts(false).loadAll(directory).orElseThrow().getContracts())
                .allSatisfy(contract -> assertThat(contract.getSchema()).isNull());
    }

    @Test
    void ambiguousLegacySchemasAndExplicitlyUnavailableSchemasAreNotUploaded() throws Exception {
        writeContract("one", null, "registry-one");
        writeContract("two", null, "registry-two");
        writeContract("three", "", "registry-three");
        Files.writeString(directory.resolve("Event-1.avsc"), "ambiguous schema");
        assertThat(messageContracts.loadAll(directory).orElseThrow().getContracts())
                .allSatisfy(contract -> assertThat(contract.getSchema()).isNull());
    }

    @Test
    void explicitUnavailableOrMissingReferenceDoesNotFallBackToStaleLegacyFile() throws Exception {
        writeContract("one", "", "registry");
        writeContract("two", "missing.avsc", "registry");
        Files.writeString(directory.resolve("Event-1.avsc"), "stale schema");
        assertThat(messageContracts.loadAll(directory).orElseThrow().getContracts())
                .allSatisfy(contract -> assertThat(contract.getSchema()).isNull());
    }

    @Test
    void readsSharedSchemaOncePerPublicationAndRefreshesNextPublication() throws Exception {
        writeContract("one", "shared.avsc", "registry");
        writeContract("two", "shared.avsc", "registry");
        Path schema = directory.resolve("shared.avsc");
        Files.writeString(schema, "first");
        MessageContracts loader = spy(new MessageContracts());
        assertThat(loader.loadAll(directory).orElseThrow().getContracts())
                .allSatisfy(contract -> assertThat(contract.getSchema()).isEqualTo("first"));
        verify(loader, times(1)).readSchemaFile(schema);
        Files.writeString(schema, "second");
        assertThat(loader.loadAll(directory).orElseThrow().getContracts())
                .allSatisfy(contract -> assertThat(contract.getSchema()).isEqualTo("second"));
        verify(loader, times(2)).readSchemaFile(schema);
    }

    @Test
    void schemaReadFailureIsReportedUnlessUploadIsDisabled() throws Exception {
        writeContract("one", "schema.avsc", "registry");
        MessageContracts loader = spy(new MessageContracts());
        doThrow(MessageContractPublishException.failedToLoadSchema(directory.resolve("schema.avsc"), new java.io.IOException("unreadable")))
                .when(loader).readSchemaFile(directory.resolve("schema.avsc"));
        assertThatThrownBy(() -> loader.loadAll(directory)).hasRootCauseMessage("unreadable");
        MessageContracts disabled = spy(new MessageContracts(false));
        assertThat(disabled.loadAll(directory).orElseThrow().getContracts()).allSatisfy(contract -> assertThat(contract.getSchema()).isNull());
        verify(disabled, never()).readSchemaFile(any());
    }

    private void writeContract(String app, String resource, String registry) throws Exception {
        Contract contract = Contract.builder().contractVersion("1.0.0").role("producer").systemName("system")
                .messageTypeName("Event").messageTypeVersion("1").registryUrl(registry).registryBranch("master")
                .registryCommit("commit").compatibilityMode("BACKWARD").appName(app).topics(new String[]{"topic"})
                .schemaResource(resource).build();
        Files.writeString(directory.resolve(app + "-contract.json"), JsonMapper.builder().build().writeValueAsString(contract));
    }

    @Test
    void loadAll_whenSchemaFileExistsForMessageTypeVersion_thenSchemaIsLoaded() throws Exception {
        String expectedSchema = Files.readString(VALID_CONTRACTS.resolve(MESSAGE_TYPE + "-1.1.0.avsc"));

        Optional<CreateMessageContractsDto> contracts = messageContracts.loadAll(VALID_CONTRACTS);

        assertThat(contracts).isPresent();
        List<MessageContractDto> contractsWithSchema = contracts.get().getContracts().stream()
                .filter(contract -> contract.getSchema() != null)
                .toList();
        assertThat(contractsWithSchema)
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.getMessageType()).isEqualTo(MESSAGE_TYPE);
                    assertThat(contract.getMessageTypeVersion()).isEqualTo("1.1.0");
                    assertThat(contract.getSchema()).isEqualTo(expectedSchema);
                });
    }

    @Test
    void loadAll_whenNoSchemaFileExistsForMessageTypeVersion_thenSchemaIsNull() {
        Optional<CreateMessageContractsDto> contracts = messageContracts.loadAll(VALID_CONTRACTS);

        assertThat(contracts).isPresent();
        assertThat(contracts.get().getContracts())
                .filteredOn(contract -> !"1.1.0".equals(contract.getMessageTypeVersion()))
                .isNotEmpty()
                .allSatisfy(contract -> assertThat(contract.getSchema()).isNull());
    }
}
