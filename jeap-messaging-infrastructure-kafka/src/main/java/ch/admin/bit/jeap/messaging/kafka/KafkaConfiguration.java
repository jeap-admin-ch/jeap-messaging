package ch.admin.bit.jeap.messaging.kafka;

import ch.admin.bit.jeap.messaging.kafka.auth.KafkaAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.bean.JeapKafkaBeanRegistrar;
import ch.admin.bit.jeap.messaging.kafka.contract.ConsumerContractInterceptor;
import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.contract.ProducerContractInterceptor;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ClusterNameHeaderInterceptor;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.CreateSerializedMessageHolder;
import ch.admin.bit.jeap.messaging.kafka.log.ConsumerLoggingInterceptor;
import ch.admin.bit.jeap.messaging.kafka.log.ProducerLoggerInterceptor;
import ch.admin.bit.jeap.messaging.kafka.metrics.ConsumerMetricsInterceptor;
import ch.admin.bit.jeap.messaging.kafka.metrics.KafkaMessagingMetrics;
import ch.admin.bit.jeap.messaging.kafka.metrics.ProducerMetricsInterceptor;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaConsumerPropertiesValidator;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.EmptyKeyDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.KafkaAvroSerdeProvider;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.tracing.KafkaTracingConfiguration;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.ListenerContainerRegistry;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.*;
import java.util.stream.Collectors;

import static ch.admin.bit.jeap.messaging.kafka.properties.PropertyRequirements.requireNonNullValue;

@AutoConfiguration(before = {KafkaAutoConfiguration.class, KafkaConsumerConfiguration.class},
        after = {KafkaTracingConfiguration.class},
        afterName = "BraveAutoConfiguration")
@EnableKafka
@Import(JeapKafkaBeanRegistrar.class)
@Slf4j
public class KafkaConfiguration {
    private final KafkaProperties kafkaProperties;
    private final TracerBridge tracerBridge;
    private final KafkaMessagingMetrics kafkaMessagingMetrics;
    private final ContractsValidator contractsValidator;
    private final BeanFactory beanFactory;
    private final JeapKafkaBeanNames beanNames;

    @Value("${spring.application.name}")
    private String applicationName;

    KafkaConfiguration(KafkaProperties kafkaProperties,
                       Optional<TracerBridge> tracerBridge,
                       ContractsValidator contractsValidator,
                       Optional<KafkaMessagingMetrics> kafkaMetrics,
                       BeanFactory beanFactory) {
        this.kafkaProperties = kafkaProperties;
        this.tracerBridge = tracerBridge.orElse(null);
        this.contractsValidator = contractsValidator;
        this.kafkaMessagingMetrics = kafkaMetrics.orElse(null);
        this.beanFactory = beanFactory;
        this.beanNames = new JeapKafkaBeanNames(kafkaProperties.getDefaultClusterName());
    }

    @PostConstruct
    private void printConfig() {
        Set<String> clusterNames = new TreeSet<>(kafkaProperties.clusterNames());

        clusterNames.forEach(clusterName ->
                log.info("Configuration for Kafka Cluster {}: Schema-Registry: '{}', Autocommit-Schemas: '{}', Consumer-Bootstrap-Server: '{}', Producer-Bootstrap-Server: '{}', Admin-Bootstrap-Server: '{}'",
                        clusterName,
                        kafkaProperties.isUseSchemaRegistry() ? kafkaProperties.getSchemaRegistryUrl(clusterName) : "None",
                        kafkaProperties.isUseSchemaRegistry() ? kafkaProperties.isAutoRegisterSchema() : "-",
                        kafkaProperties.getSecurityProtocol(clusterName) + " " + kafkaProperties.getConsumerBootstrapServers(clusterName),
                        kafkaProperties.getSecurityProtocol(clusterName) + " " + kafkaProperties.getProducerBootstrapServers(clusterName),
                        kafkaProperties.getSecurityProtocol(clusterName) + " " + kafkaProperties.getAdminClientBootstrapServers(clusterName))
        );
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    KafkaConsumerPropertiesValidator kafkaConsumerPropertiesValidator(ListenerContainerRegistry listenerContainerRegistry) {
        return new KafkaConsumerPropertiesValidator(listenerContainerRegistry, kafkaProperties);
    }

    public Map<String, Object> adminConfig(String clusterName) {
        Map<String, Object> props = commonConfig(clusterName);
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, requireNonNullValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " for admin clients",
                kafkaProperties.getAdminClientBootstrapServers(clusterName)));
        return props;
    }

    public Map<String, Object> consumerConfig(String clusterName) {
        List<Class<?>> interceptors = new ArrayList<>();
        Map<String, Object> props = commonConfig(clusterName);

        String consumerBootstrapServers = kafkaProperties.getConsumerBootstrapServers(clusterName);
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, requireNonNullValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " for consumers",
                consumerBootstrapServers));

        KafkaAvroSerdeProvider kafkaAvroSerdeProvider = (KafkaAvroSerdeProvider) beanFactory.getBean(
                beanNames.getKafkaAvroSerdeProviderBeanName(clusterName));
        props.putAll(kafkaAvroSerdeProvider.getSerdeProperties().avroDeserializerProperties(clusterName));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, applicationName);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_FUNCTION, CreateSerializedMessageHolder.class);

        props.put(ClusterNameHeaderInterceptor.CLUSTER_NAME_CONFIG, clusterName);
        interceptors.add(ClusterNameHeaderInterceptor.class);
        interceptors.add(ConsumerContractInterceptor.class);
        interceptors.add(ConsumerLoggingInterceptor.class);
        props.put(ConsumerLoggingInterceptor.CLUSTER_NAME_CONFIG, clusterName);

        props.put(ConsumerContractInterceptor.CONTRACTS_VALIDATOR, contractsValidator);
        props.put(ConsumerContractInterceptor.ALLOW_NO_CONTRACT_EVENTS, kafkaProperties.isConsumeWithoutContractAllowed());
        props.put(ConsumerContractInterceptor.SILENT_IGNORE_NO_CONTRACT_EVENTS, kafkaProperties.isSilentIgnoreWithoutContract());
        if (tracerBridge != null) {
            props.put(ConsumerLoggingInterceptor.TRACER_BRIDGE, tracerBridge);
        }
        if (kafkaProperties.isExposeMessageKeyToConsumer()) {
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            props.put(ErrorHandlingDeserializer.KEY_FUNCTION, CreateSerializedMessageHolder.class);
        } else {
            // Don't make message keys available to consumers in order to prevent misusing Avro keys as data containers
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, EmptyKeyDeserializer.class);
        }
        if (kafkaMessagingMetrics != null) {
            props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, consumerBootstrapServers);
            props.put(ConsumerMetricsInterceptor.METER_REGISTRY, kafkaMessagingMetrics);
            props.put(ConsumerMetricsInterceptor.APPLICATION_NAME, applicationName);
            interceptors.add(ConsumerMetricsInterceptor.class);
        }
        props.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, commaSeparatedClassList(interceptors));
        return props;
    }

    public Map<String, Object> producerConfig(String clusterName) {
        List<Class<?>> interceptors = new ArrayList<>();
        Map<String, Object> props = commonConfig(clusterName);

        String producerBootstrapServers = kafkaProperties.getProducerBootstrapServers(clusterName);
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, requireNonNullValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " for producers",
                producerBootstrapServers));

        KafkaAvroSerdeProvider kafkaAvroSerdeProvider = (KafkaAvroSerdeProvider) beanFactory.getBean(
                beanNames.getKafkaAvroSerdeProviderBeanName(clusterName));
        props.putAll(kafkaAvroSerdeProvider.getSerdeProperties().avroSerializerProperties(clusterName));

        props.put(ProducerContractInterceptor.ALLOW_NO_CONTRACT_EVENTS, kafkaProperties.isPublishWithoutContractAllowed());
        props.put(ProducerContractInterceptor.ALLOW_NO_CONTRACT_EVENTS_SILENT, false);
        props.put(ProducerContractInterceptor.CONTRACTS_VALIDATOR, contractsValidator);
        interceptors.add(ProducerContractInterceptor.class);

        props.put(ProducerLoggerInterceptor.CLUSTER_NAME_CONFIG, clusterName);
        interceptors.add(ProducerLoggerInterceptor.class);

        if (kafkaMessagingMetrics != null) {
            SignaturePublisherProperties signaturePublisherProperties = beanFactory.getBean(SignaturePublisherProperties.class);

            props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, producerBootstrapServers);
            props.put(ProducerMetricsInterceptor.METER_REGISTRY, kafkaMessagingMetrics);
            props.put(ProducerMetricsInterceptor.APPLICATION_NAME, applicationName);
            props.put(ProducerMetricsInterceptor.SIGNATURE_ENABLED, signaturePublisherProperties.isSigningEnabled());
            interceptors.add(ProducerMetricsInterceptor.class);
        }
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, commaSeparatedClassList(interceptors));
        return props;
    }

    private Map<String, Object> commonConfig(String clusterName) {
        KafkaAuthProperties kafkaAuthProperties =
                (KafkaAuthProperties) beanFactory.getBean(beanNames.getAuthPropertiesBeanName(clusterName));
        return new HashMap<>(kafkaAuthProperties.authenticationProperties(clusterName));
    }

    private String commaSeparatedClassList(List<Class<?>> classes) {
        return classes.stream()
                .map(Class::getName)
                .collect(Collectors.joining(","));
    }
}
