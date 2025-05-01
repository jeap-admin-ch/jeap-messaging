package ch.admin.bit.jeap.messaging.kafka.interceptor;

import ch.admin.bit.jeap.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

import java.util.List;

@Slf4j
public class CallbackRecordInterceptor implements RecordInterceptor<Object, Object> {

    private final List<JeapKafkaMessageCallback> callbacks;

    public CallbackRecordInterceptor(List<JeapKafkaMessageCallback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        if (record.value() instanceof Message msg) {
            callbacks.forEach(cb -> Callbacks.invokeCallback(msg, cb::beforeConsume));
        }
        return record;
    }

    @Override
    public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        if (record.value() instanceof Message msg) {
            callbacks.forEach(cb -> Callbacks.invokeCallback(msg, cb::afterConsume));
        }
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        if (record.value() instanceof Message msg) {
            callbacks.forEach(cb -> Callbacks.invokeCallback(msg, cb::afterRecord));
        }
    }

}
