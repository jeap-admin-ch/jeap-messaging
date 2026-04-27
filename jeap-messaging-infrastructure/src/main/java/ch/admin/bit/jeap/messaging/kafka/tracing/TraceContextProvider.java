package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the currently active tracing context and converts it to the jEAP {@link TraceContext} POJO so downstream
 * libraries (jeap-messaging-outbox, jeap-messaging-sequential-inbox, jeap-error-handling) can persist it alongside
 * their domain data.
 */
@Slf4j
@RequiredArgsConstructor
public class TraceContextProvider {

    private final Tracer tracer;

    public TraceContext getTraceContext() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        return TraceContextTranslator.toJeapTraceContext(currentSpan.context());
    }
}
