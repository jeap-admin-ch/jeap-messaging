package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ContractMetric {
    private static final String METRIC_NAME = "jeap.messaging.contract";
    private static final String TAG_SWITCH_NAME = "switch";
    private final MeterRegistry meterRegistry;
    private final KafkaProperties kafkaProperties;
    private final ContractsProvider contractsProvider;

    @PostConstruct
    private void initialize() {
        int consumeWithoutContract = kafkaProperties.isConsumeWithoutContractAllowed() ? 1 : 0;
        int publishWithoutContract = kafkaProperties.isPublishWithoutContractAllowed() ? 1 : 0;
        int noMasterContracts = hasNoMasterContracts() ? 1 : 0;
        List<MultiGauge.Row<?>> rows = List.of(
                MultiGauge.Row.of(Tags.of(TAG_SWITCH_NAME, "consumeWithoutContract"), consumeWithoutContract),
                MultiGauge.Row.of(Tags.of(TAG_SWITCH_NAME, "publishWithoutContract"), publishWithoutContract),
                MultiGauge.Row.of(Tags.of(TAG_SWITCH_NAME, "noMasterContracts"), noMasterContracts)
        );
        MultiGauge.builder(METRIC_NAME)
                .description("Contract validation of jeap-messaging library")
                .register(meterRegistry)
                .register(rows);
    }

    private boolean hasNoMasterContracts() {
        return !getContractBranches().stream()
                //Backwards compatibility: Branch might not be set => in this case its assumed not to be "master"
                .map(b -> b == null ? "none" : b)
                .map(String::toLowerCase)
                .allMatch("master"::equals);
    }

    private Set<String> getContractBranches() {
        return contractsProvider.getContracts().stream().map(Contract::getRegistryBranch).
                filter(Objects::nonNull).
                collect(Collectors.toUnmodifiableSet());
    }
}
