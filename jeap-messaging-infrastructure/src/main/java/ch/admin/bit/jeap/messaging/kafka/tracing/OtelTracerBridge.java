package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * {@link TracerBridge} implementation on top of the OpenTelemetry API. Activates an INTERNAL span whose parent
 * context is extracted from the record's tracing headers. If no extractable parent context is present — because the
 * record carries no propagation headers (or the ones it carries are malformed) — {@link #getSpan} returns a no-op so
 * we do not emit orphan root spans for broken traceparent/b3 payloads.
 * <p>
 * Uses the OTel API directly rather than the Micrometer {@code Propagator} façade so we can inspect the extracted
 * {@link Context} via {@link io.opentelemetry.api.trace.SpanContext#isValid()}. Micrometer's {@code Propagator.extract}
 * returns only a {@code Span.Builder} with no "did extraction succeed?" signal.
 * <p>
 * The extracted {@link Context} is passed whole into the scope via {@code extracted.with(span).makeCurrent()} so any
 * baggage the configured propagators extracted (e.g. W3C {@code baggage} header) stays active while the span is open.
 * <p>
 * Span kind is {@link SpanKind#INTERNAL} — not {@link SpanKind#CONSUMER} — because this span is a trace-context
 * holder around non-listener consume-side code (Kafka {@code ConsumerInterceptor} and
 * {@code ConsumerRecordRecoverer}). Spring Kafka's listener observation already emits the real CONSUMER span for the
 * record's receive/process cycle; ours is an internal wrapper for log correlation and child spans.
 */
@Slf4j
public class OtelTracerBridge implements TracerBridge {

    private static final String DEFAULT_SPAN_NAME = "jeap-messaging.consumer-scope";

    private static final TextMapGetter<Headers> HEADER_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Headers carrier) {
            return StreamSupport.stream(carrier.spliterator(), false)
                    .map(Header::key)
                    .toList();
        }

        @Override
        public String get(Headers carrier, String key) {
            Header header = (carrier == null) ? null : carrier.lastHeader(key);
            return (header == null || header.value() == null)
                    ? null
                    : new String(header.value(), StandardCharsets.UTF_8);
        }
    };

    private final Tracer tracer;
    private final TextMapPropagator propagator;

    public OtelTracerBridge(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("jeap-messaging");
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    @SuppressWarnings("resource") // span scope must be closed by caller
    public Scope getSpan(ConsumerRecord<?, ?> consumerRecord, String spanName) {
        Context extracted = propagator.extract(Context.root(), consumerRecord.headers(), HEADER_GETTER);
        if (!Span.fromContext(extracted).getSpanContext().isValid()) {
            log.trace("Not starting a span because no valid tracing context could be extracted from the consumer record.");
            return Scope.NOOP;
        }
        Span span = tracer.spanBuilder(Objects.requireNonNullElse(spanName, DEFAULT_SPAN_NAME))
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(extracted)
                .startSpan();
        log.trace("Started child span with traceId = {}, spanId = {}.",
                span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
        io.opentelemetry.context.Scope otelScope = extracted.with(span).makeCurrent();
        return () -> {
            otelScope.close();
            span.end();
        };
    }
}
