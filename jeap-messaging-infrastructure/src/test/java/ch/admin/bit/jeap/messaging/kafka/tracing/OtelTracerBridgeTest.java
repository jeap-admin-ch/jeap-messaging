package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies bridge-specific behavior only. The many flavours of invalid W3C/B3 headers that used to live here are the
 * OTel propagators' responsibility and are covered by their own test suites; we trust the propagator to return an
 * invalid {@code SpanContext} on parse failure, and our job is only to turn that into a NOOP scope.
 */
class OtelTracerBridgeTest {

    private static final String DEFAULT_SPAN_NAME = "jeap.testspan";

    private InMemorySpanExporter spanExporter;
    private SdkTracerProvider sdkTracerProvider;
    private OtelTracerBridge bridge;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        bridge = new OtelTracerBridge(buildOpenTelemetry(W3CTraceContextPropagator.getInstance()));
    }

    @AfterEach
    void tearDown() {
        sdkTracerProvider.close();
    }

    @Test
    void getSpan_whenRecordHasTraceparentHeader_emitsChildSpanWithSameTraceId() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String parentSpanId = "00f067aa0ba902b7";
        ConsumerRecord<String, String> consumerRecord = recordWithHeaders(
                header("traceparent", "00-" + traceId + "-" + parentSpanId + "-01"));

        try (TracerBridge.Scope _ = bridge.getSpan(consumerRecord, DEFAULT_SPAN_NAME)) {
            // span active, then auto-closed
        }

        assertThat(spanExporter.getFinishedSpanItems())
                .hasSize(1)
                .first()
                .satisfies(s -> {
                    assertThat(s.getTraceId()).isEqualTo(traceId);
                    assertThat(s.getParentSpanId()).isEqualTo(parentSpanId);
                    assertThat(s.getKind()).isEqualTo(SpanKind.INTERNAL);
                    assertThat(s.getName()).isEqualTo(DEFAULT_SPAN_NAME);
                });
    }

    @Test
    void getSpan_whenNameIsNull_usesDefaultSpanName() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String parentSpanId = "00f067aa0ba902b7";
        ConsumerRecord<String, String> consumerRecord = recordWithHeaders(
                header("traceparent", "00-" + traceId + "-" + parentSpanId + "-01"));

        try (TracerBridge.Scope _ = bridge.getSpan(consumerRecord, null)) {
            // span active, then auto-closed
        }

        assertThat(spanExporter.getFinishedSpanItems())
                .hasSize(1)
                .first()
                .satisfies(s -> assertThat(s.getName()).isEqualTo("jeap-messaging.consumer-scope"));
    }

    @Test
    void getSpan_whenRecordHasNoPropagationHeaders_returnsNoopAndEmitsNoSpan() {
        ConsumerRecord<String, String> consumerRecord = recordWithHeaders();

        try (TracerBridge.Scope autoClosed = bridge.getSpan(consumerRecord, DEFAULT_SPAN_NAME)) {
            assertThat(autoClosed).isSameAs(TracerBridge.Scope.NOOP);
        }

        assertThat(spanExporter.getFinishedSpanItems()).isEmpty();
    }

    @Test
    void getSpan_whenTraceparentHeaderMalformed_returnsNoopAndEmitsNoSpan() {
        // We rely on the propagator to reject invalid headers by returning an invalid SpanContext. For such cases,
        // this test asserts that the bridge returns NOOP instead of starting an orphan root span.
        ConsumerRecord<String, String> consumerRecord = recordWithHeaders(
                header("traceparent", "00-deadbeef-cafebabe-01"));

        try (TracerBridge.Scope autoClosed = bridge.getSpan(consumerRecord, DEFAULT_SPAN_NAME)) {
            assertThat(autoClosed).isSameAs(TracerBridge.Scope.NOOP);
        }

        assertThat(spanExporter.getFinishedSpanItems()).isEmpty();
    }

    @Test
    void getSpan_withBaggageOnRecord_baggageIsActiveInScope() {
        // With a composite W3C trace-context + baggage propagator, a record carrying both headers must produce a
        // span whose scope carries the extracted baggage. The child span inherits the parent context; the
        // extracted baggage must also be made current for the duration of the scope, so downstream code can read
        // it via Baggage.current(). This is the specific reason the bridge uses extracted.with(span).makeCurrent()
        // rather than span.makeCurrent() — the latter would lose baggage.
        OtelTracerBridge baggageBridge = new OtelTracerBridge(buildOpenTelemetry(
                TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance())));
        ConsumerRecord<String, String> consumerRecord = recordWithHeaders(
                header("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"),
                header("baggage", "tenant=acme,user-id=42"));

        try (TracerBridge.Scope _ = baggageBridge.getSpan(consumerRecord, DEFAULT_SPAN_NAME)) {
            Baggage baggage = Baggage.current();
            assertThat(baggage.getEntryValue("tenant")).isEqualTo("acme");
            assertThat(baggage.getEntryValue("user-id")).isEqualTo("42");
        }
    }

    private OpenTelemetry buildOpenTelemetry(TextMapPropagator propagator) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(propagator))
                .build();
    }

    private static Header header(String name, String value) {
        return new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private static ConsumerRecord<String, String> recordWithHeaders(Header... headers) {
        RecordHeaders recordHeaders = new RecordHeaders();
        for (Header h : headers) {
            recordHeaders.add(h);
        }
        return new ConsumerRecord<>("topic", 0, 0L, 0L,
                TimestampType.CREATE_TIME,
                0, 0, "key", "value", recordHeaders, Optional.empty());
    }
}
