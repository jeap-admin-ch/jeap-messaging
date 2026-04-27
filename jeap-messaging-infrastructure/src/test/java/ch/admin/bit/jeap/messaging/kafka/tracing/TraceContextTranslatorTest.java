package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextTranslatorTest {

    private static final int HEX_RADIX = 16;
    private static final String TRACE_ID_HIGH_HEX = "4bf92f3577b34da6";
    private static final String TRACE_ID_LOW_HEX = "a3ce929d0e0e4736";
    private static final String TRACE_ID = TRACE_ID_HIGH_HEX + TRACE_ID_LOW_HEX;
    private static final String SPAN_ID = "00f067aa0ba902b7";
    private static final String PARENT_SPAN_ID = "0123456789abcdef";
    private static final String ZERO_SPAN_ID = "0000000000000000";

    private SdkTracerProvider sdkTracerProvider;
    private Tracer tracer;


    @BeforeEach
    @SuppressWarnings("resource") // openTelemetry will be closed in tearDown
    void setUp() {
        sdkTracerProvider = SdkTracerProvider.builder().build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .build();
        tracer = new OtelTracer(openTelemetry.getTracer("test"), new OtelCurrentTraceContext(), event -> { });
    }

    @AfterEach
    void tearDown() {
        sdkTracerProvider.close();
    }

    @Test
    void toJeapTraceContext_splitsTraceIdIntoHighAndLowLongs() {
        io.micrometer.tracing.TraceContext otelContext = tracer.traceContextBuilder()
                .traceId(TRACE_ID)
                .spanId(SPAN_ID)
                .parentId(PARENT_SPAN_ID)
                .sampled(true)
                .build();

        TraceContext jeap = TraceContextTranslator.toJeapTraceContext(otelContext);

        assertThat(jeap.getTraceIdHigh()).isEqualTo(Long.parseUnsignedLong(TRACE_ID_HIGH_HEX, HEX_RADIX));
        assertThat(jeap.getTraceId()).isEqualTo(Long.parseUnsignedLong(TRACE_ID_LOW_HEX, HEX_RADIX));
        assertThat(jeap.getSpanId()).isEqualTo(Long.parseUnsignedLong(SPAN_ID, HEX_RADIX));
        assertThat(jeap.getParentSpanId()).isEqualTo(Long.parseUnsignedLong(PARENT_SPAN_ID, HEX_RADIX));
        assertThat(jeap.getTraceIdString()).isEqualTo(TRACE_ID);
        assertThat(jeap.getSampled()).isTrue();
    }

    @Test
    void toJeapTraceContext_zeroParentIdBecomesNull() {
        io.micrometer.tracing.TraceContext otelContext = tracer.traceContextBuilder()
                .traceId(TRACE_ID)
                .spanId(SPAN_ID)
                .parentId(ZERO_SPAN_ID)
                .sampled(true)
                .build();

        TraceContext jeap = TraceContextTranslator.toJeapTraceContext(otelContext);

        assertThat(jeap.getParentSpanId()).isNull();
    }

    @Test
    void toJeapTraceContext_preservesUnsampledFlag() {
        io.micrometer.tracing.TraceContext otelContext = tracer.traceContextBuilder()
                .traceId(TRACE_ID)
                .spanId(SPAN_ID)
                .sampled(false)
                .build();

        TraceContext jeap = TraceContextTranslator.toJeapTraceContext(otelContext);

        assertThat(jeap.getSampled()).isFalse();
    }

    @Test
    void roundTrip_preservesAllFieldsSampled(){
        io.micrometer.tracing.TraceContext original = tracer.traceContextBuilder()
                .traceId(TRACE_ID)
                .spanId(SPAN_ID)
                .parentId(PARENT_SPAN_ID)
                .sampled(true)
                .build();

        TraceContext jeap = TraceContextTranslator.toJeapTraceContext(original);
        io.micrometer.tracing.TraceContext roundTripped = TraceContextTranslator.toMicrometerTraceContext(tracer, jeap);

        assertThat(roundTripped.traceId()).isEqualTo(original.traceId());
        assertThat(roundTripped.spanId()).isEqualTo(original.spanId());
        assertThat(roundTripped.parentId()).isEqualTo(original.parentId());
        assertThat(roundTripped.sampled()).isEqualTo(original.sampled());
    }

    @Test
    void roundTrip_preservesUnsampled() {
        io.micrometer.tracing.TraceContext original = tracer.traceContextBuilder()
                .traceId(TRACE_ID)
                .spanId(SPAN_ID)
                .sampled(false)
                .build();

        TraceContext jeap = TraceContextTranslator.toJeapTraceContext(original);
        io.micrometer.tracing.TraceContext roundTripped = TraceContextTranslator.toMicrometerTraceContext(tracer, jeap);

        assertThat(roundTripped.sampled()).isFalse();
    }

    @Test
    void toMicrometerTraceContext_nullSampledFlagDefaultsToTrue_forBackwardCompatibility() {
        TraceContext jeap = new TraceContext(
                Long.parseUnsignedLong(TRACE_ID_HIGH_HEX, HEX_RADIX),
                Long.parseUnsignedLong(TRACE_ID_LOW_HEX, HEX_RADIX),
                Long.parseUnsignedLong(SPAN_ID, HEX_RADIX),
                null,
                TRACE_ID,
                null);

        io.micrometer.tracing.TraceContext result = TraceContextTranslator.toMicrometerTraceContext(tracer, jeap);

        assertThat(result.sampled()).isTrue();
    }

    @Test
    void toMicrometerTraceContext_reconstructsTraceIdFromLongPair_whenStoredStringIsAbsent() {
        TraceContext jeap = new TraceContext(
                Long.parseUnsignedLong(TRACE_ID_HIGH_HEX, HEX_RADIX),
                Long.parseUnsignedLong(TRACE_ID_LOW_HEX, HEX_RADIX),
                Long.parseUnsignedLong(SPAN_ID, HEX_RADIX),
                null,
                null,
                Boolean.TRUE);

        io.micrometer.tracing.TraceContext result = TraceContextTranslator.toMicrometerTraceContext(tracer, jeap);

        assertThat(result.traceId()).isEqualTo(TRACE_ID);
        assertThat(result.spanId()).isEqualTo(SPAN_ID);
    }
}
