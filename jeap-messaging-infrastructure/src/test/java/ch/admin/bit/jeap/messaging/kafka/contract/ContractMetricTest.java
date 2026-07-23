package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractMetricTest {

    private static final String METRIC_NAME = "jeap.messaging.contract";
    private static final String TAG_SWITCH_NAME = "switch";

    @Test
    void initialize_registersGaugeRowsForAllContractSwitches() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setConsumeWithoutContractAllowed(true);
        kafkaProperties.setPublishWithoutContractAllowed(false);
        kafkaProperties.setSilentIgnoreWithoutContract(true);
        ContractsProvider contractsProvider = mock(ContractsProvider.class);
        when(contractsProvider.getContracts()).thenReturn(List.of());
        ContractMetric contractMetric = new ContractMetric(meterRegistry, kafkaProperties, contractsProvider);

        ReflectionTestUtils.invokeMethod(contractMetric, "initialize");

        assertThat(switchGaugeValue(meterRegistry, "consumeWithoutContract")).isEqualTo(1);
        assertThat(switchGaugeValue(meterRegistry, "publishWithoutContract")).isEqualTo(0);
        assertThat(switchGaugeValue(meterRegistry, "silentIgnoreWithoutContract")).isEqualTo(1);
        assertThat(switchGaugeValue(meterRegistry, "noMasterContracts")).isEqualTo(0);
    }

    private static double switchGaugeValue(SimpleMeterRegistry meterRegistry, String switchName) {
        return meterRegistry.get(METRIC_NAME)
                .tag(TAG_SWITCH_NAME, switchName)
                .gauge()
                .value();
    }
}
