package ch.admin.bit.jeap.messaging.kafka.tracing;

import org.apache.kafka.clients.consumer.ConsumerRecord;


public interface TracerBridge {

    TracerBridge.TracerBridgeElement getSpan(ConsumerRecord<?, ?> record);

    void backupOriginalTraceContext(ConsumerRecord<?, ?> record);

    void restoreOriginalTraceContext(ConsumerRecord<?, ?> record);

    interface TracerBridgeElement extends AutoCloseable {
        @Override
        void close();
    }

}
