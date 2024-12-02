package ch.admin.bit.jeap.messaging.kafka.tracing;

import brave.Tracer;
import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = BraveAutoConfiguration.class)
@ConditionalOnClass({Tracer.class, KafkaTracing.class})
@ConditionalOnBean({Tracer.class, Tracing.class})
public class KafkaTracingConfiguration {

    @Bean
    SleuthTracerBridge sleuthTracerBridge(Tracer tracer, KafkaTracing kafkaTracing){
        return new SleuthTracerBridge(tracer, kafkaTracing.messagingTracing());
    }

    @Bean
    TraceContextProvider traceContextProvider(KafkaTracing kafkaTracing){
        return new TraceContextProvider(kafkaTracing.messagingTracing());
    }

    @Bean
    TraceContextUpdater traceContextUpdater(KafkaTracing kafkaTracing){
        return new TraceContextUpdater(kafkaTracing.messagingTracing());
    }

    @Bean
    TracingKafkaTemplateFactory tracingKafkaTemplateFactory(KafkaTracing kafkaTracing){
        return new TracingKafkaTemplateFactory(kafkaTracing.messagingTracing());
    }

    @Bean
    KafkaTracing kafkaTracing(Tracing tracing) {
        return KafkaTracing.create(tracing);
    }

    @Bean
    JeapKafkaTracing jeapKafkaTracing(KafkaTracing kafkaTracing) {
        return new JeapKafkaTracing(kafkaTracing);
    }

}
