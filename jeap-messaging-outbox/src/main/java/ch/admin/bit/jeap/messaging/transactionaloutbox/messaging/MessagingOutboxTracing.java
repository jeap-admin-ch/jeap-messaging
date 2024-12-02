package ch.admin.bit.jeap.messaging.transactionaloutbox.messaging;

import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContext;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextUpdater;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxTraceContext;
import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.OutboxTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class MessagingOutboxTracing implements OutboxTracing {

    private final Optional<TraceContextProvider> traceContextProvider;

    private final Optional<TraceContextUpdater> traceContextUpdater;

    @Override
    public OutboxTraceContext retrieveCurrentTraceContext() {
        if (traceContextProvider.isPresent()) {
            final TraceContext traceContext = traceContextProvider.get().getTraceContext();
            if (traceContext != null) {
                final OutboxTraceContext outboxTraceContext = OutboxTraceContext.builder()
                        .traceIdHigh(traceContext.getTraceIdHigh())
                        .traceId(traceContext.getTraceId())
                        .spanId(traceContext.getSpanId())
                        .parentSpanId(traceContext.getParentSpanId())
                        .traceIdString(traceContext.getTraceIdString())
                        .build();
                log.debug("Tracing information (traceId={}) found in context. Returning trace context {}", outboxTraceContext.getTraceIdString(), outboxTraceContext);
                return outboxTraceContext;
            }
            else {
                log.debug("No tracing information available (trace context is null).");
            }
        }
        else {
            log.debug("No tracing information available (no trace context provider present).");
        }
        return null;
    }

    @Override
    public void updateCurrentTraceContext(OutboxTraceContext outboxTraceContext){
        if (traceContextUpdater.isPresent() && outboxTraceContext != null) {
            log.debug("Original trace context found on the message to send (traceId={}). Overriding the current tracing context with the tracing context {}", outboxTraceContext.getTraceIdString(), outboxTraceContext);
            traceContextUpdater.get().setTraceContext(new TraceContext(outboxTraceContext.getTraceIdHigh(), outboxTraceContext.getTraceId(), outboxTraceContext.getSpanId(), outboxTraceContext.getParentSpanId(), outboxTraceContext.getTraceIdString()));
        }
    }
}
