package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@JeapMessageConsumerContract(appName = "jme-messaging-receiverpublisher-service", topic = "jme-messaging-create-declaration", value = JmeCreateDeclarationCommand.TypeRef.class)
public class TestGlueConfig {
}
