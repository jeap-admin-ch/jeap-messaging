package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Activates a jEAP {@link TraceContext} as the current tracing context on this thread. Used by downstream libraries
 * (jeap-messaging-outbox's relay, jeap-messaging-sequential-inbox's buffered-message replay, jeap-error-handling's
 * failed-event resender) to re-establish the trace context of a previously-persisted message at the time of
 * (re-)sending so the produced span is correlated back to the originating trace.
 * <p>
 * Callers must close the returned {@link TraceContextScope} (preferably via try-with-resources) to restore the
 * previous context. Not closing leaks the OpenTelemetry {@code Context} scope on the thread-local stack. The old
 * Brave-based implementation returned {@code void} and relied on a second {@code setTraceContext(original)} call to
 * "restore"; under OTel this pattern does not restore and grows the stack, hence the return-type change.
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("resource")
public class TraceContextUpdater {

    private final Tracer tracer;

    public TraceContextScope setTraceContext(TraceContext traceContext) {
        io.micrometer.tracing.TraceContext micrometerContext = TraceContextTranslator.toMicrometerTraceContext(tracer, traceContext);
        CurrentTraceContext.Scope scope = tracer.currentTraceContext().newScope(micrometerContext);
        if (log.isDebugEnabled()) {
            log.debug("TraceContext updated with traceId = {}, spanId = {}, parentId = {}.",
                    micrometerContext.traceId(), micrometerContext.spanId(), micrometerContext.parentId());
        }
        return scope::close;
    }
}
