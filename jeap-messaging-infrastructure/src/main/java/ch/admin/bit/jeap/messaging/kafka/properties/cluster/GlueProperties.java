package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import lombok.Data;

import java.net.URI;

@Data
public class GlueProperties {

    /**
     * Name of the Glue registry to use. If set, the Glue integration will be enabled, and confluent schema registry
     * support will be disabled.
     */
    String registryName;
    /**
     * AWS Region of the Glue registry. Mandatory property.
     */
    String region;
    /**
     * If set, the Token to access Glue will be retrieved from the AWS Security Token Service using the provided
     * IAM Role ARN with an AssumeRole request. This is typically required if the Glue registry resides
     * in a different AWS Account than the application accessing it.
     */
    String assumeIamRoleArn;
    /**
     * AWS Security Token Service Endpoint. Override to use regional endpoint. See
     * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_temp_enable-regions.html">STS docs</a>
     */
    URI stsEndpoint = null;
    /**
     * Connection / socket timeout for the AWS STS service used to assume a role
     */
    int stsClientTimeoutSeconds = 30;
    /**
     * Override endpoint URI of the Glue registry. Mostly meant for tests.
     */
    URI endpoint;

    public void validateProperties(boolean isConfluentRegistryActive) {
        if (!isActive()) {
            // Glue schema registry is not active
            return;
        }

        if (isConfluentRegistryActive) {
            throw new IllegalArgumentException("Confluent schema registry and Glue schema registry cannot be active at " +
                    "the same time for a kafka cluster configuration. Please configure either " +
                    "jeap.messaging.kafka.cluster.<name>.aws.registryName OR jeap.messaging.kafka.cluster.<name>.schemaRegistryUrl");
        }

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException(
                    "Property %s.cluster.<name>.aws.glue.region cannot be blank - please provide a value"
                            .formatted(KafkaProperties.PREFIX));
        }
    }

    public boolean isActive() {
        return registryName != null;
    }

    public boolean useAssumeRoleForAuth() {
        return assumeIamRoleArn != null;
    }
}
