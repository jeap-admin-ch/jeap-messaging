package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(AwsMskAuthBeanRegistrar.class)
public class AwsMskKafkaAuthConfiguration {

}
