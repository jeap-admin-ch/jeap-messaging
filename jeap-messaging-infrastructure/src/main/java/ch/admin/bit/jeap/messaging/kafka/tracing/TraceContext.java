package ch.admin.bit.jeap.messaging.kafka.tracing;

import lombok.Value;

@Value
public class TraceContext {

    Long traceIdHigh;

    Long traceId;

    Long spanId;

    Long parentSpanId;

    String traceIdString;

    /**
     * Sampling decision inherited from the original span. {@code null} means "unknown" — applies to contexts
     * persisted by older jEAP versions that did not record this flag.
     */
    Boolean sampled;
}
