package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.BeanReferenceMessageKeyV2;
import ch.admin.bit.jme.test.JmeSimpleTestV2Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class JmeDeclarationCreatedEventCustomDeserializerPropertiesConsumer {

    public static final String TOPIC_NAME = "some-topic";
    public static final String OTHER_TOPIC_NAME = "some-other-topic";

    @Getter
    private BeanReferenceMessageKeyV2 lastMessageKey;

    @Autowired(required = false)
    private final List<MessageListener<JmeSimpleTestV2Event>> testEventV2Processors;

    @Autowired(required = false)
    private final List<MessageListener<JmeDeclarationCreatedEvent>> testEventProcessors;

    @TestKafkaListener(topics = TOPIC_NAME,
            properties = {
                    "specific.avro.value.type=ch.admin.bit.jme.test.JmeSimpleTestV2Event",
                    KafkaTestConstants.TEST_CONSUMER_DISABLE_CONTRACT_CHECK_PROPERTY
            })
    public void consumeWithCustomValueDeserializer(JmeSimpleTestV2Event event, Acknowledgment ack) {
        testEventV2Processors.forEach(testEventProcessor -> testEventProcessor.receive(event));
        ack.acknowledge();
    }

    @TestKafkaListener(topics = OTHER_TOPIC_NAME,
            properties = {
                    "specific.avro.key.type=ch.admin.bit.jme.test.BeanReferenceMessageKeyV2",
                    KafkaTestConstants.TEST_CONSUMER_DISABLE_CONTRACT_CHECK_PROPERTY
            })
    public void consumeWithCustomKeyDeserializer(ConsumerRecord consumerRecord, Acknowledgment ack) {
        testEventProcessors.forEach(testEventProcessor -> testEventProcessor.receive((JmeDeclarationCreatedEvent) consumerRecord.value()));
        lastMessageKey = (BeanReferenceMessageKeyV2) consumerRecord.key();
        ack.acknowledge();
    }

}
