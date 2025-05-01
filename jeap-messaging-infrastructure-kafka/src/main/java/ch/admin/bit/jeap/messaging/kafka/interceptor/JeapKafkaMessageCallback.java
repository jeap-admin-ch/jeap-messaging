package ch.admin.bit.jeap.messaging.kafka.interceptor;

import ch.admin.bit.jeap.messaging.model.Message;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

public interface JeapKafkaMessageCallback {

    /**
     * Invoked on the sender thread before a message is sent to the broker.
     *
     * @see org.apache.kafka.clients.producer.ProducerInterceptor#onSend(ProducerRecord)
     */
    void onSend(Message message);

    /**
     * Invoked on the kafka consumer thread before a message is consumed by the listener.
     * Note that all consumer methods are re-entrant: The sequential inbox might invoke messages consumers in the
     * thread of the message consumer that triggered buffered messages. Implementations should thus keep a stack of
     * invocations. afterRecord() is always guaranteed to be invoked after a beforeConsume() invocation.
     * @see org.springframework.kafka.listener.RecordInterceptor#intercept(ConsumerRecord, Consumer)
     */
    void beforeConsume(Message message);

    /**
     * Invoked on the kafka consumer thread after a message has been successfully consumed by the listener.
     * @see org.springframework.kafka.listener.RecordInterceptor#success(ConsumerRecord, Consumer)
     */
    void afterConsume(Message message);

    /**
     * Invoked on the kafka consumer thread after a message has been consumed by the listener (sucessfully or not).
     * @see org.springframework.kafka.listener.RecordInterceptor#afterRecord(ConsumerRecord, Consumer)
     */
    void afterRecord(Message message);

}
