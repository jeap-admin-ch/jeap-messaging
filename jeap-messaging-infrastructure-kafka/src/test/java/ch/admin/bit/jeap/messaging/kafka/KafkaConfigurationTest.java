package ch.admin.bit.jeap.messaging.kafka;

import ch.admin.bit.jeap.messaging.kafka.auth.KafkaAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


class KafkaConfigurationTest {

    private static final String CONSUMER_BOOTSTRAP_SERVERS = "consumer";
    private static final String PRODUCER_BOOTSTRAP_SERVERS = "producer";
    private static final String ADMIN_CLIENT_BOOTSTRAP_SERVERS = "adminclient";

    @Test
    void publisherConfig_shouldThrowOnMissingValue() {
        KafkaProperties kafkaProperties = mock(KafkaProperties.class);
        doReturn(KafkaProperties.DEFAULT_CLUSTER).when(kafkaProperties).getDefaultClusterName();
        KafkaConfiguration kafkaConfiguration = createConfiguration(kafkaProperties);

        assertThatThrownBy(() -> kafkaConfiguration.producerConfig(KafkaProperties.DEFAULT_CLUSTER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Required kafka config property bootstrap.servers for producers is set to null");
    }

    @Test
    void testBootstrapServersConfigurationFromProperties() {
        KafkaProperties props = mockBootstrapServersKafkaProperties();

        KafkaConfiguration config = createConfiguration(props);

        String clusterName = KafkaProperties.DEFAULT_CLUSTER;
        assertThat(config.consumerConfig(clusterName).get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(CONSUMER_BOOTSTRAP_SERVERS);
        assertThat(config.producerConfig(clusterName).get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(PRODUCER_BOOTSTRAP_SERVERS);
        assertThat(config.adminConfig(clusterName).get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(ADMIN_CLIENT_BOOTSTRAP_SERVERS);
    }

    private KafkaConfiguration createConfiguration(KafkaProperties kafkaProperties) {
        KafkaAvroSerdeProperties configurationMock = mock(KafkaAvroSerdeProperties.class);
        doReturn(Map.of()).when(configurationMock).avroDeserializerProperties(KafkaProperties.DEFAULT_CLUSTER);
        doReturn(Map.of()).when(configurationMock).avroSerializerProperties(KafkaProperties.DEFAULT_CLUSTER);
        KafkaAvroSerdeProvider providerMock = mock(KafkaAvroSerdeProvider.class);
        doReturn(configurationMock).when(providerMock).getSerdeProperties();
        BeanFactory beanFactoryMock = mock(BeanFactory.class);
        doReturn(providerMock).when(beanFactoryMock).getBean("kafkaAvroSerdeProvider");
        KafkaAuthProperties authPropertiesMock = mock(KafkaAuthProperties.class);
        doReturn(authPropertiesMock).when(beanFactoryMock).getBean("kafkaAuthProperties");

        return new KafkaConfiguration(
                kafkaProperties,
                Optional.empty(),
                mock(ContractsValidator.class),
                Optional.empty(),
                beanFactoryMock);
    }

    private KafkaProperties mockBootstrapServersKafkaProperties() {
        KafkaProperties kafkaProperties = mock(KafkaProperties.class);
        when(kafkaProperties.getConsumerBootstrapServers(KafkaProperties.DEFAULT_CLUSTER)).thenReturn(CONSUMER_BOOTSTRAP_SERVERS);
        when(kafkaProperties.getProducerBootstrapServers(KafkaProperties.DEFAULT_CLUSTER)).thenReturn(PRODUCER_BOOTSTRAP_SERVERS);
        when(kafkaProperties.getAdminClientBootstrapServers(KafkaProperties.DEFAULT_CLUSTER)).thenReturn(ADMIN_CLIENT_BOOTSTRAP_SERVERS);
        when(kafkaProperties.getSecurityProtocol(KafkaProperties.DEFAULT_CLUSTER)).thenReturn(SecurityProtocol.PLAINTEXT);
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        return kafkaProperties;
    }
}
