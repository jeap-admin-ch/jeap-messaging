package ch.admin.bit.jeap.messaging.sequentialinbox.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequentialInboxService;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

@RequiredArgsConstructor
@Slf4j
public class KafkaSequentialInboxMessageListener implements AcknowledgingMessageListener<AvroMessageKey, AvroMessage> {

    private final SequentialInboxMessageHandler messageHandler;
    private final SequentialInboxService sequentialInboxService;

    @Override
    public void onMessage(ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, Acknowledgment acknowledgment) {
        sequentialInboxService.handleMessage(consumerRecord, messageHandler, acknowledgment);
    }
}
