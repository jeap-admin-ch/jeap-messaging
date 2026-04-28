package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
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
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the producer→consumer observation chain that replaced Brave's
 * {@code KafkaTracing} wrapper: Spring Kafka's Micrometer Observation on the {@link KafkaTemplate} emits a
 * PRODUCER span, the listener-side Observation emits a CONSUMER span, and both share the same trace id — i.e. the
 * W3C {@code traceparent} header injected by the producer observation is extracted by the listener observation.
 * This is the main end-to-end behaviour the Brave → OpenTelemetry migration had to preserve, so it is guarded here
 * against a real Embedded Kafka rather than with mocked internals.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {TestConfig.class, ObservationConfigurationIT.InMemoryExporterConfig.class}, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "management.tracing.sampling.probability=1.0"})
@AutoConfigureTracing
@Slf4j
@DirtiesContext
class ObservationConfigurationIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> testEventProcessor;

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory;

    @Autowired
    private KafkaTemplate<?, ?> kafkaTemplateForReflection;

    @Autowired
    private InMemorySpanExporter spanExporter;

    @Test
    void observationIsEnabled() throws NoSuchFieldException, IllegalAccessException {
        assertTrue(kafkaListenerContainerFactory.getContainerProperties().isObservationEnabled(),
                "Listener-side observation must be programmatically enabled by JeapKafkaBeanDefinitionFactory when a "
                        + "TracerBridge bean is present — without it the CONSUMER span below would never be emitted.");
        Field observationEnabled = kafkaTemplateForReflection.getClass().getDeclaredField("observationEnabled");
        observationEnabled.setAccessible(true);
        assertTrue(observationEnabled.getBoolean(kafkaTemplateForReflection));
        observationEnabled.setAccessible(false);
    }

    @Test
    void kafkaRoundTrip_emitsCorrelatedProducerAndConsumerSpans() {
        spanExporter.reset();

        sendSync(TestEventConsumer.TOPIC_NAME, JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .message("hello-observation")
                .build());

        // Wait until the listener has delivered the event to the test-side MessageListener, so we know the listener
        // observation has opened and closed its span. Without this the assertion below can race against the consumer.
        Mockito.verify(testEventProcessor, Mockito.timeout(Duration.ofSeconds(10).toMillis()))
                .receive(Mockito.any(JmeDeclarationCreatedEvent.class));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<SpanData> producerSpans = spanExporter.getFinishedSpanItems().stream()
                    .filter(s -> s.getKind() == SpanKind.PRODUCER)
                    .toList();
            List<SpanData> consumerSpans = spanExporter.getFinishedSpanItems().stream()
                    .filter(s -> s.getKind() == SpanKind.CONSUMER)
                    .toList();

            assertThat(producerSpans)
                    .as("Expected a PRODUCER-kind span for the KafkaTemplate send. Missing means observation on the "
                            + "template didn't fire — check JeapKafkaBeanDefinitionFactory's setObservationEnabled path.")
                    .isNotEmpty();
            assertThat(consumerSpans)
                    .as("Expected a CONSUMER-kind span for the @KafkaListener invocation. Missing means the listener "
                            + "container factory didn't have observation-enabled at start time — this is the main "
                            + "Brave-replacement behaviour.")
                    .isNotEmpty();

            Set<String> producerTraceIds = producerSpans.stream().map(SpanData::getTraceId).collect(Collectors.toSet());
            Set<String> consumerTraceIds = consumerSpans.stream().map(SpanData::getTraceId).collect(Collectors.toSet());
            assertThat(consumerTraceIds)
                    .as("Listener observation must extract the W3C traceparent from the record so its CONSUMER span "
                            + "shares the trace id of the PRODUCER span. Different trace ids mean the propagator "
                            + "failed to extract — the end-to-end chain is broken.")
                    .containsAnyElementsOf(producerTraceIds);

            // 32-hex ids prove we're actually on OTel (Brave emitted 16-hex ids); guards against a silent regression
            // if the bridge were ever swapped back.
            assertThat(producerSpans.getFirst().getTraceId()).hasSize(32).matches("[0-9a-f]{32}");
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
            // SimpleSpanProcessor flushes on span end so test assertions see spans immediately; the default
            // BatchSpanProcessor would only flush on SDK shutdown and deadlock the Awaitility wait above.
            return SimpleSpanProcessor.create(exporter);
        }
    }
}
