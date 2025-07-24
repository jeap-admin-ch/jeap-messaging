package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.kafka.auth.KafkaAuthProperties;
import ch.admin.bit.jeap.messaging.kafka.auth.KafkaTrustStoreUtility;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.ClusterProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.MskAuthProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.msk.auth.iam.IAMClientCallbackHandler;
import software.amazon.msk.auth.iam.IAMLoginModule;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

public class AwsMskKafkaAuthProperties implements KafkaAuthProperties {

    private static final String AWS_MSK_IAM = "AWS_MSK_IAM";

    private final KafkaProperties kafkaProperties;

    @Value("${spring.application.name}")
    private String sessionName;

    @Value("${jeap.aws.rolesanywhere.enabled:false}")
    private boolean rolesAnywhereEnabled;

    public AwsMskKafkaAuthProperties(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Configuration properties for Kafka clients using AWS MSK with IAM authentication. See
     * <a href="https://github.com/aws/aws-msk-iam-auth/blob/main/README.md">README</a>.
     */
    @Override
    public Map<String, Object> authenticationProperties(String clusterName) {
        ClusterProperties clusterProperties = kafkaProperties.clusterProperties(clusterName).orElseThrow();
        MskAuthProperties mskAuthProperties = clusterProperties.getAws().getMsk();

        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name());

        KafkaTrustStoreUtility.setTrustStorePropertiesFromSystemProperties(props, false, false);

        props.put(SaslConfigs.SASL_MECHANISM, AWS_MSK_IAM);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, createSaslJaasConfig(mskAuthProperties));

        if (rolesAnywhereEnabled) {
            props.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, IAMRolesAnywhereCallbackHandler.class.getName());
        } else {
            props.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, IAMClientCallbackHandler.class.getName());
        }

        return props;
    }

    private String createSaslJaasConfig(MskAuthProperties mskAuthProperties) {
        if (mskAuthProperties.useAssumeRoleForAuth()) {
            if (!hasText(sessionName)) {
                throw new IllegalStateException("Session name is required when using assume-role IAM auth - please set spring.application.name");
            }
            return String.format("%s required awsRoleArn=\"%s\" awsRoleSessionName=\"%s\" awsStsRegion=\"%s\";",
                    IAMLoginModule.class.getName(),
                    mskAuthProperties.getAssumeIamRoleArn(),
                    sessionName,
                    mskAuthProperties.getRegion());
        } else {
            return String.format("%s required;",
                    IAMLoginModule.class.getName());
        }
    }
}
