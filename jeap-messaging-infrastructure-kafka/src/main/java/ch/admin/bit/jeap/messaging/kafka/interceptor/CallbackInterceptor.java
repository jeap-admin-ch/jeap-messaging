package ch.admin.bit.jeap.messaging.kafka.interceptor;

import ch.admin.bit.jeap.messaging.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class CallbackInterceptor implements ProducerInterceptor<Object, Object> {

    public static final String CALLBACK_LIST = "jeap.kafka.callback.list";

    private List<JeapKafkaMessageCallback> callbacks = List.of();

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs) {
        List<JeapKafkaMessageCallback> configuredCallbacks = (List<JeapKafkaMessageCallback>) configs.get(CALLBACK_LIST);
        if (configuredCallbacks != null) {
            this.callbacks = configuredCallbacks;
        }
    }

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        if (record.value() instanceof Message msg) {
            callbacks.forEach(cb -> invokeCallbacks(msg, cb::onSend));
        }
        return record;
    }

    private void invokeCallbacks(Message msg, Consumer<Message> method) {
        try {
            method.accept(msg);
        } catch (Exception e) {
            log.warn("Exception in callback", e);
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    }

    @Override
    public void close() {
    }

}
