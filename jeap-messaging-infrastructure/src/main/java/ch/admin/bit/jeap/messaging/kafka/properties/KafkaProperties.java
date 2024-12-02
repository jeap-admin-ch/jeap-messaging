package ch.admin.bit.jeap.messaging.kafka.properties;

import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.SslProperties;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Stream.empty;
import static org.springframework.util.StringUtils.hasText;

@ConfigurationProperties(prefix = KafkaProperties.PREFIX)
public class KafkaProperties implements InitializingBean {

    public static final String PREFIX = "jeap.messaging.kafka";
    public static final String DEFAULT_CLUSTER = "default";

    static final List<String> DEFAULT_STACK_TRACE_EXCLUSION_PATTERNS = List.of(
            "^java.base/.*",
            "^org.springframework.aop..*",
            ".*\\$\\$FastClassByCGLIB\\$\\$.*",
            ".*\\$\\$EnhancerBySpringCGLIB\\$\\$.*",
            ".*\\$\\$EnhancerByCGLIB\\$\\$.*",
            ".*\\$\\$SpringCGLIB\\$\\$.*");
    @Setter
    private Map<String, ClusterProperties> cluster = new LinkedHashMap<>();

    @Getter
    private String defaultClusterName;

    @Getter
    private String defaultProducerClusterOverride;

    @Override
    public void afterPropertiesSet() {
        boolean mockRegistryActivated = !useSchemaRegistry;
        boolean confluentSchemaRegistryActive = mockRegistryActivated || hasText(schemaRegistryUrl);

        // If no cluster is configuration under ".cluster:", fallback to the legacy single-cluster properties that
        // are located directly under jeap.messaging.kafka.
        if (cluster.isEmpty()) {
            ClusterProperties props = createDefaultClusterProperties();
            props.validateProperties(confluentSchemaRegistryActive);
            defaultClusterName = DEFAULT_CLUSTER;
        } else {
            defaultClusterName = validateAndMarkDefaultCluster();
            cluster.values().forEach(clusterProperties -> clusterProperties.validateProperties(confluentSchemaRegistryActive));
            // Override default producer cluster if configured
            defaultProducerClusterOverride = cluster.entrySet().stream()
                    .filter(entry -> entry.getValue().isDefaultProducerClusterOverride())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }
    }

    public boolean hasDefaultProducerClusterOverride() {
        return defaultProducerClusterOverride != null;
    }

    public String getDefaultProducerClusterName() {
        if (hasDefaultProducerClusterOverride()) {
            return defaultProducerClusterOverride;
        } else {
            return defaultClusterName;
        }
    }

    private String validateAndMarkDefaultCluster() {
        long defaultClusterCount = cluster.values().stream().filter(ClusterProperties::isDefaultCluster).count();
        if (defaultClusterCount == 0) {
            // When no default cluster is explicitly configured, the first cluster configuration is marked as default
            // This is safe as spring ensures key ordering (https://github.com/spring-projects/spring-boot/commit/beb7cb4b81bc5ec1c9e50ff7a02c29a613c428cf)
            String firstCluster = cluster.keySet().iterator().next();
            cluster.get(firstCluster).setDefaultCluster(true);
        } else if (defaultClusterCount > 1) {
            throw new IllegalStateException(
                    "When configuring kafka cluster(s), only one cluster can be marked as the default cluster " +
                            "using jeap.kafka.messaging.cluster.<name>.default-cluster=true.");
        }
        long defaultProducerClusterOverrideCount = cluster.values().stream().filter(ClusterProperties::isDefaultProducerClusterOverride).count();
        if (defaultProducerClusterOverrideCount > 1) {
            throw new IllegalStateException(
                    "When configuring kafka cluster(s), at most one cluster can be marked as the default producer cluster " +
                            "using jeap.kafka.messaging.cluster.<name>.default-producer-cluster-override=true.");
        }
        return cluster.entrySet().stream()
                .filter(e -> e.getValue().isDefaultCluster())
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow();
    }

    private ClusterProperties createDefaultClusterProperties() {
        ClusterProperties props = new ClusterProperties();
        props.setDefaultCluster(true);

        props.setBootstrapServers(bootstrapServers);
        props.setAdminClientBootstrapServers(adminClientBootstrapServers);
        props.setConsumerBootstrapServers(consumerBootstrapServers);
        props.setProducerBootstrapServers(producerBootstrapServers);
        props.setSecurityProtocol(securityProtocol);
        props.setUsername(username);
        props.setPassword(password);

        props.setSchemaRegistryPassword(schemaRegistryPassword);
        props.setSchemaRegistryUrl(schemaRegistryUrl);
        props.setSchemaRegistryUsername(schemaRegistryUsername);
        props.setErrorTopicName(errorTopicName);
        return props;
    }

    /**
     * A bootstrap-server of the Kafka-Cluster
     */
    @Setter
    private String bootstrapServers;
    @Setter
    private String consumerBootstrapServers;
    @Setter
    private String producerBootstrapServers;
    @Setter
    private String adminClientBootstrapServers;

    /**
     * The security protocol to be used, usually {@link SecurityProtocol#SASL_SSL} on CF or
     * {@link SecurityProtocol#SASL_PLAINTEXT} for local kafka. For backwards compatibility reasons, this defaults to
     * SASL_SSL even when using AWS MSK IAM Auth.
     */
    @Setter
    private SecurityProtocol securityProtocol = SecurityProtocol.SASL_SSL;
    /**
     * The username if {@link SecurityProtocol#SASL_PLAINTEXT} or {@link SecurityProtocol#SASL_SSL} is used
     */
    @Setter
    private String username;
    /**
     * The password if {@link SecurityProtocol#SASL_PLAINTEXT} or {@link SecurityProtocol#SASL_SSL} is used
     */
    @Setter
    private String password;
    /**
     * The URL of the schema registry to be used. Only needed if useSchemaRegistry is set to true.
     * Enables the usage of the confluent schema registry if set.
     */
    @Setter
    private String schemaRegistryUrl;
    /**
     * Username to be used to authenticate against the confluent schema registry.
     */
    @Setter
    private String schemaRegistryUsername;
    /**
     * Password to be used to authenticate against the confluent schema registry
     */
    @Setter
    private String schemaRegistryPassword;
    /**
     * The name of the topics to send error to
     */
    @Setter
    private String errorTopicName;

    /**
     * Automatically push schemas to the schema registry when writing a message in a new schema
     */
    @Setter
    @Getter
    private boolean autoRegisterSchema = true;
    /**
     * Use the schema registry or not. If set to 'false' a local schema registry mock is used
     */
    @Setter
    @Getter
    private boolean useSchemaRegistry = true;
    /**
     * Normally the message key cannot be read by a consuming service, to prevent dependencies on the message key
     * There never shall be any important information only in the message key. However, if you  want to read the message
     * key this can be switched on here
     */
    @Setter
    @Getter
    private boolean exposeMessageKeyToConsumer = false;
    /**
     * The name of the system. This is needed to set the system name in generated errors
     */
    @Setter
    @Getter
    private String systemName;
    /**
     * The name of the service. This is needed to set the service name in generated errors
     */
    @Setter
    @Getter
    private String serviceName;
    /**
     * Normally you are only allowed to publish events for whom an event contract exists
     * in the event registry. However, e.g. in dev-environments or locally you might want to play around with local
     * events, then you can switch this off here.
     */
    @Setter
    @Getter
    private boolean publishWithoutContractAllowed = false;
    /**
     * Normally you are only allowed to consume events for whom an event contract exists
     * in the event registry. However, e.g. in dev-environments or locally you might want to play around with local
     * events, then you can switch this off here.
     */
    @Setter
    @Getter
    private boolean consumeWithoutContractAllowed = false;
    /**
     * Normally if an event for whom you no contract exists in the event registry is published you are notified by a
     * Warning in the log. You can switch this off by setting this flag. In these cases such events are dropped (if
     * consumeWithoutContractAllowed is false) or consumed (if consumeWithoutContractAllowed is true) without
     * log statement
     */
    @Setter
    @Getter
    private boolean silentIgnoreWithoutContract = false;

    /**
     * When an error cannot be sent to the error service the system will automatically retry. This setting
     * configures the wait time between the retries in milliseconds
     */
    @Setter
    @Getter
    private long errorServiceRetryIntervalMs = 5000;

    /**
     * When an error cannot be sent to the error service the system will automatically retry. This setting
     * configures the maximal number of retries
     */
    @Setter
    @Getter
    private long errorServiceRetryAttempts = 5;

    /**
     * When the length of the StackTrace of the exception caught by the error service is larger than the maximum value defined,
     * it is truncated to prevent the error message sent from being too large for the topic.
     * Default value is 7000
     */
    @Setter
    @Getter
    private int errorEventStackTraceMaxLength = 7000;

    /**
     * Enabling or disabling the computing of stack trace hashes for exceptions caught by the error handler.
     */
    @Setter
    @Getter
    private boolean errorStackTraceHashEnabled = true;


    /**
     * When computing the stack trace hash of an exception caught by the error handler, stack trace elements will be
     * ignored if their FQCN with method name matches one of the preconfigured regular expressions in this list.
     */
    @Setter
    @Getter
    private List<String> errorStackTraceHashDefaultExclusionPatterns = DEFAULT_STACK_TRACE_EXCLUSION_PATTERNS;

    /**
     * When computing the stack trace hash of an exception caught by the error handler, stack trace elements will be
     * ignored if their FQCN with method name matches one of the regular expressions provided in this list in addition
     * to the preconfigured regular expressions in errorStackTraceDefaultExclusionPatterns.
     */
    @Setter
    @Getter
    private List<String> errorStackTraceHashAdditionalExclusionPatterns = List.of();

    /**
     * Disable the support for encrypting selected message types. Use for integration tests and development purposes only.
     * Prohibited on environments 'abn' and 'prod'.
     */
    @Setter
    @Getter
    private boolean messageTypeEncryptionDisabled = false;

    public Optional<ClusterProperties> clusterProperties(String clusterName) {
        if (cluster.isEmpty()) {
            return Optional.of(createDefaultClusterProperties());
        } else {
            return Optional.ofNullable(cluster.get(clusterName));
        }
    }

    public Set<String> clusterNames() {
        if (cluster.isEmpty()) {
            return Set.of(DEFAULT_CLUSTER);
        } else {
            return Set.copyOf(cluster.keySet());
        }
    }

    public String getErrorTopicName(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getErrorTopicName)
                .filter(StringUtils::hasText)
                .orElse(errorTopicName);
    }

    public SecurityProtocol getSecurityProtocol(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getSecurityProtocol)
                .orElse(null);
    }

    public String getSchemaRegistryUrl(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getSchemaRegistryUrl)
                .orElse(null);
    }

    public String getSchemaRegistryUsername(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getSchemaRegistryUsername)
                .orElse(null);

    }

    public String getSchemaRegistryPassword(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getSchemaRegistryPassword)
                .orElse(null);
    }

    public String getUsername(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getUsername)
                .orElse(null);
    }

    public String getPassword(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getPassword)
                .orElse(null);
    }

    public String getBootstrapServers(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getBootstrapServers)
                .orElse(null);
    }

    public String getConsumerBootstrapServers(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getConsumerBootstrapServers)
                .orElse(null);
    }

    public String getProducerBootstrapServers(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getProducerBootstrapServers)
                .orElse(null);

    }

    public String getAdminClientBootstrapServers(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getAdminClientBootstrapServers)
                .orElse(null);
    }

    public SslProperties getSslProperties(String clusterName) {
        return clusterProperties(clusterName)
                .map(ClusterProperties::getSsl)
                .orElse(null);
    }

    /**
     * Returns the stacktrace exclusion patterns configured in errorStackTraceHashDefaultExclusionPatterns and
     *  errorStackTraceHashAdditionalExclusionPatterns combined.
     */
    public List<String> getErrorStackTraceHashExclusionPatterns() {
        return Stream.concat(
                errorStackTraceHashDefaultExclusionPatterns != null ? errorStackTraceHashDefaultExclusionPatterns.stream() : empty(),
                errorStackTraceHashAdditionalExclusionPatterns != null ? errorStackTraceHashAdditionalExclusionPatterns.stream() : empty()).
                toList();
    }

}
