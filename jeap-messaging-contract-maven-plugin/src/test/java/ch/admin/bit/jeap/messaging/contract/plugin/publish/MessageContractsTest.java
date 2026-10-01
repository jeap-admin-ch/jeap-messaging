package ch.admin.bit.jeap.messaging.contract.plugin.publish;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.CreateMessageContractsDto;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.dto.MessageContractDto;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MessageContractsTest {

    private static final Path VALID_CONTRACTS = Path.of("src/test/resources/validContracts");
    private static final String MESSAGE_TYPE = "JmeCreateDeclarationCommand";

    private final MessageContracts messageContracts = new MessageContracts();

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
