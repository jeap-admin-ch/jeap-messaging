package ch.admin.bit.jeap.messaging.kafka.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaPropertiesTest {

    @Test
    void getBootstrapServers_legacyPropertyNames() {
        KafkaProperties props = createKafkaConfigurationProperties(env -> env
                .withProperty("jeap.messaging.kafka.bootstrap-servers", "host"));

        assertThat(props.clusterProperties(KafkaProperties.DEFAULT_CLUSTER).orElseThrow().getBootstrapServers())
                .isEqualTo("host");
    }

    @Test
    void getBootstrapServers_legacyPropertyNamesPropertyRefresh() {
        KafkaProperties props = createKafkaConfigurationProperties(env -> env
                .withProperty("jeap.messaging.kafka.bootstrap-servers", "host"));

        assertThat(props.clusterProperties(KafkaProperties.DEFAULT_CLUSTER).orElseThrow().getBootstrapServers())
                .isEqualTo("host");

        assertDoesNotThrow(props::afterPropertiesSet, "Property refresh works without throwing");

        assertThat(props.clusterProperties(KafkaProperties.DEFAULT_CLUSTER).orElseThrow().getBootstrapServers())
                .isEqualTo("host");
        assertThat(props.clusterNames())
                .containsOnly(KafkaProperties.DEFAULT_CLUSTER);
    }

    @Test
    void multiClusterConfiguration() {
        KafkaProperties props = createKafkaConfigurationProperties(env -> env
                .withProperty("jeap.messaging.kafka.cluster.default.bootstrap-servers", "host-default")
                .withProperty("jeap.messaging.kafka.cluster.aws.bootstrap-servers", "host-aws"));

        assertThat(props.clusterProperties(KafkaProperties.DEFAULT_CLUSTER).orElseThrow().getBootstrapServers())
                .isEqualTo("host-default");
        assertThat(props.clusterProperties("aws").orElseThrow().getBootstrapServers())
                .isEqualTo("host-aws");
    }

    @Test
    void multiClusterConfiguration_requiresSingleDefaultCluster() {
        assertThatThrownBy(() ->
                createKafkaConfigurationProperties(env -> env
                        .withProperty("jeap.messaging.kafka.cluster.first.default-cluster", "true")
                        .withProperty("jeap.messaging.kafka.cluster.second.default-cluster", "true")))
                .hasMessageContaining("only one cluster can be marked as the default");
    }

    @Test
    void defaultProducerCluster_WhenMultiClusterConfigWithProducerClusterOverride_ThenProducerClusterOverrideName() {
        final KafkaProperties kafkaProperties = createKafkaConfigurationProperties(env -> env
                .withProperty("jeap.messaging.kafka.cluster.first.default-cluster", "true")
                .withProperty("jeap.messaging.kafka.cluster.second.default-producer-cluster-override", "true"));

        final String defaultProducerClusterName = kafkaProperties.getDefaultProducerClusterName();

        assertThat(defaultProducerClusterName).isEqualTo("second");
    }

    @Test
    void defaultProducerCluster_WhenMultiClusterConfigWithoutProducerClusterOverride_ThenDefaultClusterName() {
        final KafkaProperties kafkaProperties = createKafkaConfigurationProperties(env -> env
                .withProperty("jeap.messaging.kafka.cluster.first.default-cluster", "true")
                .withProperty("jeap.messaging.kafka.cluster.second.default-producer-cluster-override", "false"));

        final String defaultProducerClusterName = kafkaProperties.getDefaultProducerClusterName();

        assertThat(defaultProducerClusterName).isEqualTo("first");
    }

    @Test
    void defaultProducerCluster_WhenNonMultiClusterConfig_ThenDefaultClusterConstant() {
        final KafkaProperties kafkaProperties = createKafkaConfigurationProperties(env -> env
                .withProperty("jeap.messaging.kafka.systemName", "test"));

        final String defaultProducerClusterName = kafkaProperties.getDefaultProducerClusterName();

        assertThat(defaultProducerClusterName).isEqualTo(KafkaProperties.DEFAULT_CLUSTER);
    }

    @Test
    void getErrorStackTraceExclusionPatterns() {
        // Check defaults
        KafkaProperties props = new KafkaProperties();
        assertThat(props.getErrorStackTraceHashExclusionPatterns()).isEqualTo(KafkaProperties.DEFAULT_STACK_TRACE_EXCLUSION_PATTERNS);

        // Check additional patterns
        final List<String> additionalExclusionPatterns = List.of("foo", "bar");
        props.setErrorStackTraceHashAdditionalExclusionPatterns(additionalExclusionPatterns);
        assertThat(props.getErrorStackTraceHashExclusionPatterns()).
                isEqualTo(concat(KafkaProperties.DEFAULT_STACK_TRACE_EXCLUSION_PATTERNS, additionalExclusionPatterns));

        // Check overriding defaults
        final List<String> overriddenDefaultExclusionPatterns = List.of("overridden");
        props.setErrorStackTraceHashDefaultExclusionPatterns(overriddenDefaultExclusionPatterns);
        assertThat(props.getErrorStackTraceHashExclusionPatterns()).
                isEqualTo(concat(overriddenDefaultExclusionPatterns, additionalExclusionPatterns));
    }

    private List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }


    private static KafkaProperties createKafkaConfigurationProperties(Function<MockEnvironment, MockEnvironment> environmentFunction) {
        MockEnvironment environment = environmentFunction.apply(new MockEnvironment());
        KafkaProperties kafkaProperties = new KafkaProperties();
        Bindable<KafkaProperties> propertiesBindable = Bindable.ofInstance(kafkaProperties);
        KafkaProperties props = Binder.get(environment)
                .bind("jeap.messaging.kafka", propertiesBindable)
                .get();
        props.afterPropertiesSet();
        return props;
    }
}
