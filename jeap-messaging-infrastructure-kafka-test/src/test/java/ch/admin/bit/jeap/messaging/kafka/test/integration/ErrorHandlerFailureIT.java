package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceFailedHandler;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestEventConsumer;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.annotation.DirtiesContext;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=ErrorHandlerFailureIT",
        "jeap.messaging.kafka.errorServiceRetryAttempts=2",
        "jeap.messaging.kafka.errorServiceRetryIntervalMs=100",
})
@DirtiesContext
class ErrorHandlerFailureIT extends KafkaIntegrationTestBase {
    //Register some event listener
    @MockBean
    private MessageListener<JmeDeclarationCreatedEvent> testEventProcessor;
    @MockBean
    private ErrorServiceFailedHandler errorServiceFailedHandler;
    @SpyBean
    private KafkaTemplate<?, ?> kafkaTemplate;
    @Mock
    private CompletableFuture<SendResult<Void, MessageProcessingFailedEvent>> completableFuture;
    @Mock
    private SendResult<Void, MessageProcessingFailedEvent> sendResult;

    @Test
    void noError() {
        //Event processor will run normally
        doNothing().when(testEventProcessor).receive(any());

        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);

        //Listener is executed and no error is generated
        verify(testEventProcessor, timeout(TEST_TIMEOUT)).receive(any());
        verifyNoMoreInteractions(errorServiceFailedHandler);
    }

    @Test
    @SneakyThrows
    void errorHandlerWorks() {
        //Event handler will generate an exception but error handler works
        doThrow(new RuntimeException()).when(testEventProcessor).receive(any());
        doReturn(completableFuture).when(kafkaTemplate).send(eq("errorTopic"), any(), any());

        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);

        //ErrorServiceFailedHandler is not executed
        Thread.sleep(TEST_TIMEOUT);
        verifyNoInteractions(errorServiceFailedHandler);
    }

    @Test
    void errorHandlerFailsInstantly() {
        //Event handler will generate an exception and error handler throws an exception
        doThrow(new RuntimeException()).when(testEventProcessor).receive(any());
        doThrow(new RuntimeException()).when(kafkaTemplate).send(eq("errorTopic"), any(), any());

        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);

        //ErrorServiceFailedHandler is invoked
        verify(errorServiceFailedHandler, timeout(TEST_TIMEOUT)).handle(any());
    }

    @Test
    @SneakyThrows
    void errorHandlerFailsLazy() {
        //Event handler will generate an exception and the sending returns a failed future
        doThrow(new RuntimeException()).when(testEventProcessor).receive(any());
        doReturn(completableFuture).when(kafkaTemplate).send(eq("errorTopic"), any(), any());
        doThrow(new RuntimeException()).when(completableFuture).get();

        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);

        //ErrorServiceFailedHandler is invoked
        verify(errorServiceFailedHandler, timeout(TEST_TIMEOUT)).handle(any());
    }

    @Test
    @SneakyThrows
    void errorHandlerRetry() {
        //Event handler will generate an exception and the sending returns a failed future once then works
        doThrow(new RuntimeException()).when(testEventProcessor).receive(any());
        doReturn(completableFuture).when(kafkaTemplate).send(eq("errorTopic"), any(), any());
        doThrow(new RuntimeException())
                .doReturn(sendResult)
                .when(completableFuture).get();

        AvroMessage message = JmeDeclarationCreatedEventBuilder.create().idempotenceId("idempotenceId").message("message").build();
        sendSync(TestEventConsumer.TOPIC_NAME, message);

        //ErrorServiceFailedHandler is not executed
        Thread.sleep(TEST_TIMEOUT);
        verifyNoInteractions(errorServiceFailedHandler);
    }
}
