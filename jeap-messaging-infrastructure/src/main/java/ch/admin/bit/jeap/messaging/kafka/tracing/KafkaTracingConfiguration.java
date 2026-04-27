package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Wires the jEAP tracing adapters when the Micrometer Tracing OTel bridge is active — meaning the Micrometer
 * {@link Tracer} in the context is backed by OpenTelemetry (not Brave). The gate on
 * {@code io.micrometer.tracing.otel.bridge.OtelTracer} is what makes that precise: that class only ships in
 * {@code micrometer-tracing-bridge-otel}, so its presence is a reliable signal that the OTel bridge — and the
 * OTel-backed {@code Tracer} / {@code Propagator} beans — are what the application is wired with. Without this
 * gate, a mixed runtime with the Brave bridge plus an unrelated {@code OpenTelemetry} bean on the classpath would
 * match and produce a bridge that opens OTel scopes the Brave-backed providers can't see.
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration"
})
@ConditionalOnClass(value = {Tracer.class, OpenTelemetry.class}, name = "io.micrometer.tracing.otel.bridge.OtelTracer")
@ConditionalOnBean({Tracer.class, OpenTelemetry.class})
public class KafkaTracingConfiguration {

    @Bean
    OtelTracerBridge otelTracerBridge(OpenTelemetry openTelemetry) {
        return new OtelTracerBridge(openTelemetry);
    }

    @Bean
    TraceContextProvider traceContextProvider(Tracer tracer) {
        return new TraceContextProvider(tracer);
    }

    @Bean
    TraceContextUpdater traceContextUpdater(Tracer tracer) {
        return new TraceContextUpdater(tracer);
    }
}
