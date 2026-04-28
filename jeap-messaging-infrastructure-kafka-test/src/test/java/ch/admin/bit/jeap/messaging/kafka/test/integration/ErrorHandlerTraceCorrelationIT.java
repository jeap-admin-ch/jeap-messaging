package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestEventConsumer;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the jEAP-specific workaround for Spring Kafka's error-handler lifecycle: the listener-level Observation scope
 * has already been closed when {@link org.springframework.kafka.listener.DefaultErrorHandler} invokes the
 * {@link org.springframework.kafka.listener.ConsumerRecordRecoverer}. {@code ErrorServiceSender} re-extracts the
 * trace context from the failed record and opens a new scope so the produced {@code MessageProcessingFailedEvent}
 * stays correlated to the original message's trace. Without this, the error-event send starts a fresh trace and the
 * two are unlinkable.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {TestConfig.class, ErrorHandlerTraceCorrelationIT.InMemoryExporterConfig.class}, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "management.tracing.sampling.probability=1.0"})
@AutoConfigureTracing
@DirtiesContext
class ErrorHandlerTraceCorrelationIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> testEventProcessor;

    @Autowired
    private InMemorySpanExporter spanExporter;

    @Test
    void errorEventSend_sharesTraceIdWithOriginalProducer() {
        Mockito.doThrow(new RuntimeException("expected failure for trace-correlation test"))
                .when(testEventProcessor).receive(Mockito.any());
        spanExporter.reset();

        AvroMessage message = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("payload")
                .build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);

        // Spring Kafka's producer observation opens a new trace for the original send and injects W3C traceparent
        // onto the record. When DefaultErrorHandler invokes ErrorServiceSender outside the listener observation scope,
        // OtelTracerBridge#getSpan re-extracts that same traceparent and activates it, so the error-event
        // send becomes a child in the same trace.
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<SpanData> producerSpans = spanExporter.getFinishedSpanItems().stream()
                    .filter(s -> s.getKind() == SpanKind.PRODUCER)
                    .toList();
            assertThat(producerSpans)
                    .as("Expected a PRODUCER span for the original send AND one for the ErrorServiceSender's MessageProcessingFailedEvent.")
                    .hasSizeGreaterThanOrEqualTo(2);
            Set<String> traceIds = producerSpans.stream().map(SpanData::getTraceId).collect(Collectors.toSet());
            assertThat(traceIds)
                    .as("Original-send PRODUCER span and error-event PRODUCER span must share a trace id. "
                            + "Different trace ids mean ErrorServiceSender failed to re-extract the record's trace context.")
                    .hasSize(1);
        });
    }

    @TestConfiguration
    static class InMemoryExporterConfig {

        @Bean
        @Primary
        SpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }

        @Bean
        SpanProcessor testSpanProcessor(SpanExporter exporter) {
            return SimpleSpanProcessor.create(exporter);
        }
    }
}
