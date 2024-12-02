package ch.admin.bit.jeap.messaging.kafka.serde.glue.config;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.properties.cluster.AwsProperties;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

@AutoConfiguration
@Import(GlueSchemaRegistryBeanRegistrar.class)
public class GlueAutoConfiguration {

    @Bean
    @Conditional(GlueActiveForAnyClusterCondition.class)
    @ConditionalOnMissingBean(AwsCredentialsProvider.class)
    DefaultCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    public static class GlueActiveForAnyClusterCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            KafkaProperties kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(context.getEnvironment());
            return kafkaProperties.clusterNames().stream().anyMatch(clusterName ->
                    isGlueActive(clusterName, kafkaProperties));
        }

        private static boolean isGlueActive(String clusterName, KafkaProperties kafkaProperties) {
            AwsProperties aws = kafkaProperties.clusterProperties(clusterName).orElseThrow().getAws();
            return aws != null && aws.getGlue() != null && aws.getGlue().isActive();
        }
    }
}
