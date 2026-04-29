package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.micrometer.tracing.Span;
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

class TraceContextProviderAndUpdaterTest {

    private SdkTracerProvider sdkTracerProvider;
    private Tracer tracer;
    private TraceContextProvider provider;
    private TraceContextUpdater updater;

    @BeforeEach
    void setUp() {
        sdkTracerProvider = SdkTracerProvider.builder().build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .build();
        tracer = new OtelTracer(openTelemetry.getTracer("test"), new OtelCurrentTraceContext(), event -> { });
        provider = new TraceContextProvider(tracer);
        updater = new TraceContextUpdater(tracer);
    }

    @AfterEach
    void tearDown() {
        sdkTracerProvider.close();
    }

    @Test
    void getTraceContext_returnsNull_whenNoSpanActive() {
        assertThat(provider.getTraceContext()).isNull();
    }

    @Test
    void getTraceContext_reflectsActiveSpan() {
        Span span = tracer.nextSpan().name("test").start();
        try (Tracer.SpanInScope _ = tracer.withSpan(span)) {
            TraceContext ctx = provider.getTraceContext();
            assertThat(ctx).isNotNull();
            assertThat(ctx.getTraceIdString()).hasSize(32);
        } finally {
            span.end();
        }
    }

    @Test
    void setTraceContext_activatesContext_andScopeCloseRestoresPrevious() {
        assertThat(tracer.currentSpan()).isNull();

        TraceContext target = new TraceContext(
                Long.parseUnsignedLong("4bf92f3577b34da6", 16),
                Long.parseUnsignedLong("a3ce929d0e0e4736", 16),
                Long.parseUnsignedLong("00f067aa0ba902b7", 16),
                null,
                "4bf92f3577b34da6a3ce929d0e0e4736",
                Boolean.TRUE);

        try (TraceContextScope _ = updater.setTraceContext(target)) {
            TraceContext current = provider.getTraceContext();
            assertThat(current).isNotNull();
            assertThat(current.getTraceIdString()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        }

        // After scope close the context is cleared (no previous span was active before we opened the scope).
        assertThat(tracer.currentSpan()).isNull();
    }

    @Test
    void setTraceContext_nestedScopesRestoreInReverseOrder() {
        TraceContext outer = newContext("aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbb", "1111111111111111");
        TraceContext inner = newContext("ccccccccccccccccdddddddddddddddd", "2222222222222222");

        try (TraceContextScope _ = updater.setTraceContext(outer)) {
            assertThat(provider.getTraceContext().getTraceIdString()).isEqualTo("aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbb");
            try (TraceContextScope _ = updater.setTraceContext(inner)) {
                assertThat(provider.getTraceContext().getTraceIdString()).isEqualTo("ccccccccccccccccdddddddddddddddd");
            }
            assertThat(provider.getTraceContext().getTraceIdString()).isEqualTo("aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbb");
        }

        assertThat(tracer.currentSpan()).isNull();
    }

    @Test
    void setTraceContext_withSampledFalse_producesUnsampledChildSpans() {
        TraceContext unsampled = new TraceContext(
                Long.parseUnsignedLong("4bf92f3577b34da6", 16),
                Long.parseUnsignedLong("a3ce929d0e0e4736", 16),
                Long.parseUnsignedLong("00f067aa0ba902b7", 16),
                null,
                "4bf92f3577b34da6a3ce929d0e0e4736",
                Boolean.FALSE);

        try (TraceContextScope _ = updater.setTraceContext(unsampled)) {
            // Derive a child span: it must inherit the unsampled flag from the restored context so downstream
            // producers do not export spans that the originating trace chose to drop.
            Span child = tracer.nextSpan().name("child").start();
            try (Tracer.SpanInScope _ = tracer.withSpan(child)) {
                assertThat(child.context().sampled())
                        .as("Child of an unsampled restored context must remain unsampled.")
                        .isFalse();
            } finally {
                child.end();
            }
        }
    }

    private static TraceContext newContext(String traceIdHex32, String spanIdHex16) {
        return new TraceContext(
                Long.parseUnsignedLong(traceIdHex32.substring(0, 16), 16),
                Long.parseUnsignedLong(traceIdHex32.substring(16, 32), 16),
                Long.parseUnsignedLong(spanIdHex16, 16),
                null,
                traceIdHex32,
                Boolean.TRUE);
    }
}
