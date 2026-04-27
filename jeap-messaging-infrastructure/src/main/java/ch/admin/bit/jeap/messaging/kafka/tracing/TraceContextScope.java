package ch.admin.bit.jeap.messaging.kafka.tracing;

/**
 * Handle for an active tracing scope opened by {@link TraceContextUpdater#setTraceContext}. Close it to restore the
 * previously-active trace context on this thread — ideally via try-with-resources.
 * <p>
 * Declared as {@code AutoCloseable} with a non-throwing {@link #close()} so callers do not need to catch checked
 * exceptions. Leaving a scope unclosed leaks the OpenTelemetry thread-local context stack.
 */
public interface TraceContextScope extends AutoCloseable {

    /**
     * Shared no-op scope for callers that need to return a {@code TraceContextScope} when no tracing is active.
     * Closing it does nothing, so it is safe to use as the empty case in try-with-resources.
     */
    TraceContextScope NOOP = () -> { };

    @Override
    void close();
}
