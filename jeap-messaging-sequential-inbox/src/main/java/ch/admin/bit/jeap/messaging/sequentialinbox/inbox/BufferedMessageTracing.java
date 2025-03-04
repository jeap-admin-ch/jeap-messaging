package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContext;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextUpdater;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequentialInboxTraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
class BufferedMessageTracing {
    private final Optional<TraceContextUpdater> traceContextUpdaterBean;
    private final Optional<TraceContextProvider> traceContextProvider;

    TraceContextRestorer updateCurrentTraceContext(SequentialInboxTraceContext inboxTraceContext) {
        if (inboxTraceContext == null) {
            return new TraceContextRestorer(null);
        }
        TraceContext originalTraceContext = getOriginalTraceContext();
        updateTraceContext(inboxTraceContext);
        return new TraceContextRestorer(originalTraceContext);
    }

    private void updateTraceContext(SequentialInboxTraceContext inboxTraceContext) {
        traceContextUpdaterBean.ifPresent(traceContextUpdater -> {
            log.debug("Original trace context found on buffered message (traceId={}). Overriding the current tracing context with the tracing context {}",
                    inboxTraceContext.getTraceIdString(), inboxTraceContext);
            traceContextUpdater.setTraceContext(createTraceContext(inboxTraceContext));
        });
    }

    private TraceContext getOriginalTraceContext() {
        return traceContextProvider.map(TraceContextProvider::getTraceContext).orElse(null);
    }

    private static TraceContext createTraceContext(SequentialInboxTraceContext inboxTraceContext) {
        return new TraceContext(inboxTraceContext.getTraceIdHigh(), inboxTraceContext.getTraceId(),
                inboxTraceContext.getSpanId(), inboxTraceContext.getParentSpanId(), inboxTraceContext.getTraceIdString());
    }

    @RequiredArgsConstructor
    public class TraceContextRestorer implements Closeable {

        private final TraceContext traceContextToRestore;

        @Override
        public void close() {
            if (traceContextToRestore == null) {
                return;
            }
            traceContextUpdaterBean.ifPresent(traceContextUpdater -> {
                log.debug("Replacing trace context of buffered message with original trace context with id {}", traceContextToRestore.getTraceId());
                traceContextUpdater.setTraceContext(traceContextToRestore);
            });
        }
    }
}
