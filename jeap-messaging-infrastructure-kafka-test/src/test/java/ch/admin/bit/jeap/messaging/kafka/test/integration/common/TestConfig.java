package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(value = {"ch.admin.bit.jeap.messaging.kafka.test.integration.common"}, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = MessagingMessageConsumer.class))
@EnableAutoConfiguration
@JeapMessageProducerContract(appName = "jme-messaging-receiverpublisher-service", value = JmeDeclarationCreatedEvent.TypeRef.class)
@JeapMessageConsumerContract(appName = "jme-messaging-receiverpublisher-service", value = JmeDeclarationCreatedEvent.TypeRef.class)
@JeapMessageConsumerContract(appName = "jme-messaging-subscriber-service", value = JmeDeclarationCreatedEvent.TypeRef.class)
@JeapMessageProducerContract(appName = "jme-messaging-sender-service", value = JmeCreateDeclarationCommand.TypeRef.class, encryptionKeyId = "testKey")
public class TestConfig {
}
