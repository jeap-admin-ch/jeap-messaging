package ch.admin.bit.jeap.messaging.sequentialinbox.kafka;

import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContext;
import ch.admin.bit.jeap.messaging.kafka.tracing.TraceContextProvider;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequentialInboxTraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TraceContextFactory {

    private final Optional<TraceContextProvider> traceContextProvider;

    public SequentialInboxTraceContext currentTraceContext() {
        if (traceContextProvider.isEmpty()) {
            return SequentialInboxTraceContext.empty();
        }

        return createCurrentTraceContext(traceContextProvider.get());
    }

    private static SequentialInboxTraceContext createCurrentTraceContext(TraceContextProvider traceContextProvider) {
        TraceContext traceContext = traceContextProvider.getTraceContext();
        if (traceContext == null) {
            return SequentialInboxTraceContext.empty();
        }

        return SequentialInboxTraceContext.builder()
                .traceIdHigh(traceContext.getTraceIdHigh())
                .traceId(traceContext.getTraceId())
                .spanId(traceContext.getSpanId())
                .parentSpanId(traceContext.getParentSpanId())
                .traceIdString(traceContext.getTraceIdString()).build();
    }
}
