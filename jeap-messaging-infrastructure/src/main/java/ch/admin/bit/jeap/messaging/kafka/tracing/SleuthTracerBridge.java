package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.Span;
import brave.Tracer;
import brave.messaging.MessagingTracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.util.Iterator;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is inspired by {@link brave.kafka.clients.KafkaTracing}. It provides means to extract and activate
 * tracing information from Kafka messages. We cannot use {@link brave.kafka.clients.KafkaTracing} directly
 * because it removes the tracing headers from the messages after extraction.
 */
@Slf4j
public class SleuthTracerBridge implements TracerBridge {

    public static final String ORIGINAL_TRACE_HEADERS_PREFIX = "jeap-trace-original-";

    private final Tracer tracer;
    private final TraceContext.Extractor<Headers> extractor;
    private final TraceContext.Injector<Headers> injector;
    private final TraceContext.Extractor<Headers> originalTraceBackupExtractor;
    private final TraceContext.Injector<Headers> originalTraceBackupInjector;

    public SleuthTracerBridge(Tracer tracer, MessagingTracing messagingTracing) {
        this.tracer = tracer;
        Propagation<String> propagation = messagingTracing.tracing().propagation();
        this.extractor = propagation.extractor(SleuthTracerBridge::propagationGetter);
        this.injector = propagation.injector(SleuthTracerBridge::propagationSetter);
        this.originalTraceBackupExtractor = propagation.extractor(SleuthTracerBridge::originalTraceBackupPropagationGetter);
        this.originalTraceBackupInjector = propagation.injector(SleuthTracerBridge::originalTraceBackupPropagationSetter);
    }

    @Override
    public TracerBridgeElement getSpan(ConsumerRecord<?, ?> record) {
        TraceContextOrSamplingFlags extracted = extractor.extract(record.headers());
        if (extracted.context() != null) {
            log.trace("Tracing context for new span from consumer record is trace id = {}, span id = {}, parent id = {}.", extracted.context().traceIdString(), extracted.context().spanIdString(), extracted.context().parentIdString());
            Span span = tracer.nextSpan(extracted);
            log.trace("Starting new child span with trace id = {}, span id = {}, parent id = {}.", span.context().traceIdString(), span.context().spanIdString(), span.context().parentIdString());
            Tracer.SpanInScope spanInScope = this.tracer.withSpanInScope(span.start());
            return () -> {
                spanInScope.close();
                span.finish();
            };
        }

        log.trace("Not starting a span because no tracing context could be found in the consumer record.");
        return () -> {};
    }

    @Override
    public void backupOriginalTraceContext(ConsumerRecord<?, ?> record) {
        Headers headers = record.headers();
        TraceContextOrSamplingFlags contextOrSamplingFlagsExtracted = extractor.extract(headers);
        TraceContext context = contextOrSamplingFlagsExtracted.context();
        if (context != null) {
            log.trace("Backing up tracing context from consumer record: trace id = {}, span id = {}, parent id = {}.", context.traceIdString(), context.spanIdString(), context.parentIdString());
            originalTraceBackupInjector.inject(context, headers);
        }
    }

    @Override
    public void restoreOriginalTraceContext(ConsumerRecord<?, ?> record) {
        Headers headers = record.headers();
        TraceContextOrSamplingFlags contextOrSamplingFlagsExtracted = originalTraceBackupExtractor.extract(headers);
        TraceContext context = contextOrSamplingFlagsExtracted.context();
        if (context != null) {
            log.trace("Restoring tracing context to consumer record: trace id = {}, span id = {} and parent id = {}.", context.traceIdString(), context.spanIdString(), context.parentIdString());
            injector.inject(context, headers);
            removeOriginalTraceContextHeaders(headers);
        }
    }

    private void removeOriginalTraceContextHeaders(Headers headers) {
        Iterator<Header> i = headers.iterator();
        while(i.hasNext()) {
            String key = i.next().key();
            if ((key != null) && key.startsWith(ORIGINAL_TRACE_HEADERS_PREFIX)) {
                i.remove();
            }
        }
    }

    private static String propagationGetter(Headers headers, String name) {
        Header header = headers.lastHeader(name);
        if (header == null || header.value() == null) return null;
        return new String(header.value(), UTF_8);
    }

    private static String originalTraceBackupPropagationGetter(Headers headers, String name) {
        return propagationGetter(headers, toOriginalTraceHeaderName(name));
    }

    private static void propagationSetter(Headers headers, String name, String value) {
        try {
            headers.remove(name);
            headers.add(name, value.getBytes(UTF_8));
        } catch (IllegalStateException e) {
            log.error("Unable to set header {} in headers {}.", name, headers, e);
        }
    }

    private static void originalTraceBackupPropagationSetter(Headers headers, String name, String value) {
        propagationSetter(headers, toOriginalTraceHeaderName(name), value);
    }

    private static String toOriginalTraceHeaderName(String headername) {
        return ORIGINAL_TRACE_HEADERS_PREFIX + headername;
    }

}
