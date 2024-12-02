package ch.admin.bit.jeap.messaging.kafka.test.integration.test;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@JeapMessageConsumerContract(appName = "jme-messaging-receiverpublisher-service", value = JmeCreateDeclarationCommand.TypeRef.class)
@JeapMessageProducerContract(appName = "jme-messaging-receiverpublisher-service", value = JmeDeclarationCreatedEvent.TypeRef.class)
public class TestApp {
}
