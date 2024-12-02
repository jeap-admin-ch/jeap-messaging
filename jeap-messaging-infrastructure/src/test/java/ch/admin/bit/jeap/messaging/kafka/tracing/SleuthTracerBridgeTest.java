package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import brave.messaging.MessagingTracing;
import brave.propagation.TraceContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static ch.admin.bit.jeap.messaging.kafka.tracing.SleuthTracerBridge.ORIGINAL_TRACE_HEADERS_PREFIX;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SleuthTracerBridgeTest {

    @Test
    void testBackupAndRestoreOfOriginalTraceContext() {
        Tracing tracing = Tracing.newBuilder().build();
        MessagingTracing messageTracing = MessagingTracing.newBuilder(tracing).build();
        KafkaTracing kafkaTracing = KafkaTracing.create(messageTracing);
        Tracer tracer = messageTracing.tracing().tracer();
        SleuthTracerBridge sleuthTracerBridge = new SleuthTracerBridge(tracer, messageTracing);
        TraceContext.Injector<Headers> injector = tracing.propagation().injector(this::propagationSetter);
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 1, "key", "value");

        // Start a new trace and inject its original context into the record's headers
        Span originalSpan = tracer.nextSpan();
        TraceContext originalContext = originalSpan.context();
        injector.inject(originalContext, record.headers());

        final Headers originalHeaders = record.headers();
        final int originalContextNumHeaders = originalHeaders.toArray().length;
        assertTrue(originalContextNumHeaders > 0);
        assertTrue(Stream.of(record.headers().toArray()).noneMatch(
                header -> header.key().startsWith(ORIGINAL_TRACE_HEADERS_PREFIX)));

        // Backup the original context
        sleuthTracerBridge.backupOriginalTraceContext(record);

        final int originalAndBackupContextNumHeaders = record.headers().toArray().length;
        assertEquals(2 * originalContextNumHeaders, originalAndBackupContextNumHeaders);
        assertEquals(originalContextNumHeaders, Stream.of(record.headers().toArray())
                .filter(header -> header.key().startsWith(ORIGINAL_TRACE_HEADERS_PREFIX)).count());

        // Make the brave Kafka tracing implementation create a new child span from the record's context.
        // This will erase the original context's headers!
        kafkaTracing.nextSpan(record);

        final int afterKafkaTracingNextSpanNumHeaders = record.headers().toArray().length;
        assertEquals(originalContextNumHeaders, afterKafkaTracingNextSpanNumHeaders);
        assertTrue(Stream.of(record.headers().toArray()).allMatch(
                header -> header.key().startsWith(ORIGINAL_TRACE_HEADERS_PREFIX)));

        // Restore the original trace context
        sleuthTracerBridge.restoreOriginalTraceContext(record);

        final int afterRestoreOriginalTraceContextNumHeaders = record.headers().toArray().length;
        assertEquals(originalContextNumHeaders, afterRestoreOriginalTraceContextNumHeaders);
        assertTrue(Stream.of(record.headers().toArray()).noneMatch(
                header -> header.key().startsWith(ORIGINAL_TRACE_HEADERS_PREFIX)));
        assertEquals(originalHeaders, record.headers());
    }


    private void propagationSetter(Headers headers, String name, String value) {
        headers.remove(name);
        headers.add(name, value.getBytes(UTF_8));
    }

}
