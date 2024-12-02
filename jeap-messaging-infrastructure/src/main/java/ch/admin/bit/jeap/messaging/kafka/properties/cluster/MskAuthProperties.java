package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import lombok.Data;

@Data
public class MskAuthProperties {
    private static final String PREFIX = "jeap.messaging.kafka.cluster.<name>.aws.msk";

    /**
     * If enabled, this flag triggers using MSK IAM authentication.
     */
    boolean iamAuthEnabled;
    /**
     * If set, the Token to access Glue will be retrieved from the AWS Security Token Service using the provided
     * IAM Role ARN with an AssumeRole request. This is typically required if the Glue registry resides
     * in a different AWS Account than the application accessing it.
     */
    String assumeIamRoleArn;
    /**
     * AWS Region of the STS service for assuming roles. Mandatory only for assume role auth.
     */
    String region;

    void validateProperties() {
        if (!iamAuthEnabled) {
            // MSK IAM Auth is not enabled
            return;
        }

        if (useAssumeRoleForAuth()) {
            if (region == null) {
                throw new IllegalArgumentException(
                        "Property %s.%s cannot be blank when using assume role auth - please provide a value".formatted(PREFIX, "region"));
            }
        }
    }

    public boolean useAssumeRoleForAuth() {
        return assumeIamRoleArn != null;
    }
}
