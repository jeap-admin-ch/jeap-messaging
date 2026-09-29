package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.DefaultExceptionTemporalityResolver;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ExceptionTemporalityResolver;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestEventConsumer;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {TestConfig.class, ErrorHandlerCustomTemporalityResolverIT.CustomResolverConfig.class},
        properties = "spring.application.name=ErrorHandlerCustomTemporalityResolverIT")
@DirtiesContext
class ErrorHandlerCustomTemporalityResolverIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;
    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> testEventProcessor;
    @Autowired
    private Map<String, ExceptionTemporalityResolver> resolvers;

    @Test
    void applicationResolverReplacesDefaultResolver() {
        assertEquals(1, resolvers.size());
        ExceptionTemporalityResolver resolver = resolvers.values().iterator().next();
        assertFalse(resolver instanceof DefaultExceptionTemporalityResolver);
        assertSame(CustomResolverConfig.RESOLVER, resolver);
    }

    @Test
    void applicationResolverClassifiesCustomException() {
        Mockito.doThrow(new MyTemporaryException()).when(testEventProcessor).receive(Mockito.any());

        MessageProcessingFailedEvent resultingError = sendAndExpectError();

        assertEquals("TEMPORARY", resultingError.getReferences().getErrorType().getTemporality());
    }

    @Test
    void applicationResolverDelegatingToDefaultsKeepsDefaultClassification() {
        Mockito.doThrow(new TimeoutException("timeout")).when(testEventProcessor).receive(Mockito.any());

        MessageProcessingFailedEvent resultingError = sendAndExpectError();

        assertEquals("TEMPORARY", resultingError.getReferences().getErrorType().getTemporality());
    }

    @Test
    void applicationResolverDelegatingToDefaultsDoesNotClassifyWrappedException() {
        Mockito.doThrow(new RuntimeException(new TimeoutException("timeout"))).when(testEventProcessor).receive(Mockito.any());

        MessageProcessingFailedEvent resultingError = sendAndExpectError();

        assertEquals("UNKNOWN", resultingError.getReferences().getErrorType().getTemporality());
    }

    private MessageProcessingFailedEvent sendAndExpectError() {
        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TEST_TIMEOUT)).receive(captor.capture());
        return captor.getValue();
    }

    private static class MyTemporaryException extends RuntimeException {
    }

    @Configuration
    static class CustomResolverConfig {

        private static final DefaultExceptionTemporalityResolver DEFAULTS = new DefaultExceptionTemporalityResolver();
        static final ExceptionTemporalityResolver RESOLVER = exception -> exception instanceof MyTemporaryException
                ? Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY)
                : DEFAULTS.resolve(exception);

        @Bean
        ExceptionTemporalityResolver exceptionTemporalityResolver() {
            return RESOLVER;
        }
    }
}
