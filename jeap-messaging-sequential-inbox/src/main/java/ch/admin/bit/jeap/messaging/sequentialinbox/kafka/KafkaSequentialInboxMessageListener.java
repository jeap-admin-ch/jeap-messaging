package ch.admin.bit.jeap.messaging.sequentialinbox.kafka;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxException;
import ch.admin.bit.jeap.messaging.sequentialinbox.spring.SequentialInboxMessageHandler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class KafkaSequentialInboxMessageListener implements AcknowledgingMessageListener<AvroMessageKey, AvroMessage> {

    private final String messageType;
    private final SequentialInboxMessageHandler messageHandler;

    @Getter
    private final List<AvroMessage> events = new ArrayList<>();

    @Override
    public void onMessage(@NotNull ConsumerRecord<AvroMessageKey, AvroMessage> consumerRecord, Acknowledgment acknowledgment) {

        try {
            messageHandler.method().invoke(consumerRecord.value(), acknowledgment);
            log.debug("Handler called for message type {}", messageType);
        } catch (Exception e) {
            throw SequentialInboxException.handlerMethodCallFailed(messageHandler.method(), e);
        }

        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }
}
