package ch.admin.bit.jeap.messaging.kafka.tracing;

import lombok.Value;

@Value
public class TraceContext {

    Long traceIdHigh;

    Long traceId;

    Long spanId;

    Long parentSpanId;

    String traceIdString;
}
