package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.messaging.MessagingTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TraceContextUpdater {

    private final MessagingTracing messagingTracing;

    public void setTraceContext(TraceContext traceContext) {

        brave.propagation.TraceContext.Builder builder = brave.propagation.TraceContext
                .newBuilder()
                .spanId(traceContext.getSpanId())
                .traceId(traceContext.getTraceId())
                .parentId(traceContext.getParentSpanId());

        if (traceContext.getTraceIdHigh() != null) {
            builder.traceIdHigh(traceContext.getTraceIdHigh());
        }

        messagingTracing.tracing().currentTraceContext().newScope(builder.build());

        final brave.propagation.TraceContext currentTraceContext = messagingTracing.tracing().currentTraceContext().get();

        if (log.isDebugEnabled()) {
            log.debug("TraceContext updated with trace id = {}, trace id string = {}, span id = {}, span id string = {}, parent id = {}, parent id string = {}.",
                    currentTraceContext.traceId(), currentTraceContext.traceIdString(), currentTraceContext.spanId(), currentTraceContext.spanIdString(), currentTraceContext.parentId(), currentTraceContext.parentIdString());
        }
    }
}
