package ch.admin.bit.jeap.messaging.kafka.test.integration.selfmessaging;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.test.JmeBackwardSchemaEvolutionTestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Configuration
@ComponentScan({"ch.admin.bit.jeap.messaging.kafka.test.integration.selfmessaging"})
@EnableAutoConfiguration
@JeapMessageProducerContract(appName = "jme-self-messaging-service", value = JmeBackwardSchemaEvolutionTestEvent.TypeRef.class)
@JeapMessageConsumerContract(appName = "jme-self-messaging-service", value = ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent.TypeRef.class)
public class SelfMessagingTestConfig {

    @Autowired(required = false)
    private List<MessageListener<ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent>> testEventProcessors;

    @TestKafkaListener(topics = "jme-backward-schema-evolution-test-event", properties = "specific.avro.value.type=ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent"
    )
    public void consume(final ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent event, Acknowledgment ack) {
        testEventProcessors.forEach(testEventProcessor -> testEventProcessor.receive(event));
        ack.acknowledge();
    }

}
