package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.auth.aws.iam.properties.RolesAnywhereAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(RolesAnywhereAutoConfiguration.class)
@Import(AwsMskAuthBeanRegistrar.class)
public class AwsMskKafkaAuthConfiguration {

}