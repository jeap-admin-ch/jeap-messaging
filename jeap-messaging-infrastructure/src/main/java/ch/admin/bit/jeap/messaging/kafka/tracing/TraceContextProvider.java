package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.messaging.MessagingTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TraceContextProvider {

    private final MessagingTracing messagingTracing;

    public TraceContext getTraceContext(){
        final brave.propagation.TraceContext traceContext = messagingTracing.tracing().currentTraceContext().get();
        if (traceContext != null) {
            return new TraceContext(traceContext.traceIdHigh(), traceContext.traceId(), traceContext.spanId(), traceContext.parentId(), traceContext.traceIdString());
        }
        else {
            return null;
        }
    }
}
