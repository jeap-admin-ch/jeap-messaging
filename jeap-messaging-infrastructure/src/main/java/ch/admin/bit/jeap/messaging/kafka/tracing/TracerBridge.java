package ch.admin.bit.jeap.messaging.kafka.tracing;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Adapter between the Kafka consumer pipeline (Kafka {@link org.apache.kafka.clients.consumer.ConsumerInterceptor}
 * and Spring Kafka {@code ConsumerRecordRecoverer}) and the tracing infrastructure.
 * <p>
 * Both ConsumerInterceptor and ConsumerRecordRecoverer run outside the listener-level span that Spring Kafka opens
 * for a record. Therefore, interceptors and recoverers need a way to establish a span whose parent is extracted from
 * the record's tracing headers. {@link #getSpan} returns a {@link Scope} that activates such a span in a scope;
 * callers are expected to use it in try-with-resources.
 * <p>
 * Use {@link #NOOP} as a fallback when no real tracing bridge is wired (no tracing bridge on the classpath). Call
 * sites then don't need null-checks — the noop returns an empty scope so try-with-resources is still sound.
 */
public interface TracerBridge {

    /**
     * Bridge that always returns a no-op {@link Scope}. Use NOOP instead of null.
     */
    TracerBridge NOOP = _ -> () -> {
    };

    /**
     * Activates an INTERNAL span whose parent context is extracted from the record's tracing headers if present.
     * The caller is responsible to close the scope -> use within try-catch.
     * @param consumerRecord The Kafka record to extract the parent tracing span from.
     * @return The scope of the span as AutoCloseable
     */
    Scope getSpan(ConsumerRecord<?, ?> consumerRecord);

    /**
     * AutoCloseable handle that keeps a tracing span active until closed.
     */
    interface Scope extends AutoCloseable {
        @Override
        void close();

        Scope NOOP = () -> {
        };
    }

}
