package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import lombok.Data;
import org.apache.kafka.common.security.auth.SecurityProtocol;

@Data
public class ClusterProperties {

    private boolean defaultCluster;
    private boolean defaultProducerClusterOverride;

    /**
     * A bootstrap-server of the Kafka-Cluster
     */
    private String bootstrapServers;

    private String consumerBootstrapServers;

    private String producerBootstrapServers;

    private String adminClientBootstrapServers;

    public String getConsumerBootstrapServers() {
        if (consumerBootstrapServers != null) {
            return consumerBootstrapServers;
        } else {
            return bootstrapServers;
        }
    }

    public String getProducerBootstrapServers() {
        if (producerBootstrapServers != null) {
            return producerBootstrapServers;
        } else {
            return bootstrapServers;
        }
    }

    public String getAdminClientBootstrapServers() {
        if (adminClientBootstrapServers != null) {
            return adminClientBootstrapServers;
        } else {
            return bootstrapServers;
        }
    }

    /**
     * The security protocol to be used, usually {@link SecurityProtocol#SASL_SSL} on CF or
     * {@link SecurityProtocol#SASL_PLAINTEXT} for local kafka.
     */
    private SecurityProtocol securityProtocol = SecurityProtocol.SASL_SSL;

    /**
     * The URL of the schema registry to be used. Only needed if useSchemaRegistry is set to true. Enables the use
     * of the confluent schema registry.
     */
    private String schemaRegistryUrl;
    /**
     * Username to be used to authenticate against the confluent schema registry, Only used if schemaRegistryUrl
     * is set.
     */
    private String schemaRegistryUsername;
    /**
     * Password to be used to authenticate against the confluent schema registry, Only used if schemaRegistryUrl
     * is set.
     */
    private String schemaRegistryPassword;

    /**
     * The name of the topics to send error to
     */
    private String errorTopicName;
    /**
     * The username if {@link SecurityProtocol#SASL_PLAINTEXT} or {@link SecurityProtocol#SASL_SSL} is used
     */
    private String username;
    /**
     * The password if {@link SecurityProtocol#SASL_PLAINTEXT} or {@link SecurityProtocol#SASL_SSL} is used
     */
    private String password;

    /**
     * Sub-key for AWS MSK / Glue properties
     */
    private AwsProperties aws;

    /**
     * Sub-key for Ssl properties
     */
    private SslProperties ssl;

    public boolean awsMskIamAuthActive() {
        return aws != null && aws.getMsk() != null && aws.getMsk().isIamAuthEnabled();
    }

    public void validateProperties(boolean confluentSchemaRegistryActive) {
        if (aws != null) {
            aws.validateProperties(confluentSchemaRegistryActive);
        }
    }
}
