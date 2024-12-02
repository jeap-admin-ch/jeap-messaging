package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.SerializedMessageHolder;
import ch.admin.bit.jeap.messaging.avro.errorevent.*;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorServiceSenderTest {
    @Mock
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Mock
    private SerializedMessageHolder key;
    @Mock(lenient = true)
    private SerializedMessageHolder value;
    @Mock
    private CompletableFuture<SendResult<Void, MessageProcessingFailedEvent>> completableFuture;
    @Mock
    private BeanFactory beanFactory;
    @Captor
    private ArgumentCaptor<AvroDomainEvent> eventCaptor;
    @Captor
    private ArgumentCaptor<MessageProcessingFailedMessageKey> keyCaptor;

    private ErrorServiceSender target;

    /**
     * Argument matchers for {@link #checkMessage(Exception, ArgumentMatcher)}
     */
    private static Stream<Arguments> checkMessageSource() {
        Exception exception = new Exception("test");
        MessageHandlerExceptionInformation eventHandlerExceptionInformation = MessageHandlerException.builder()
                .cause(exception)
                .description("testDescription")
                .errorCode("TEST")
                .temporality(MessageHandlerExceptionInformation.Temporality.PERMANENT)
                .build();
        Arguments[] matchers = new Arguments[]{
                Arguments.of(exception, new LambdaByteArrayEqualsMatcher(new byte[]{0, 1}, (p, r) -> p.getOriginalKey().array())),
                Arguments.of(exception, new LambdaByteArrayEqualsMatcher(new byte[]{1, 1}, (p, r) -> p.getOriginalMessage().array())),
                Arguments.of(exception, new LambdaEqualsMatcher<>("3", (p, r) -> r.getMessage().getOffset())),
                Arguments.of(exception, new LambdaEqualsMatcher<>("1", (p, r) -> r.getMessage().getPartition())),
                Arguments.of(exception, new LambdaEqualsMatcher<>("topic", (p, r) -> r.getMessage().getTopicName())),

                Arguments.of(exception, new LambdaEqualsMatcher<>("java.lang.Exception: test", (p, r) -> p.getErrorMessage())),
                Arguments.of(exception, new LambdaNullMatcher<>((p, r) -> p.getErrorDescription())),
                Arguments.of(exception, new LambdaStartWithMatcher("ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException: java.lang.Exception: test", (p, r) -> p.getStackTrace())),
                Arguments.of(exception, new LambdaEqualsMatcher<>("UNKNOWN_EXCEPTION", (p, r) -> r.getErrorType().getCode())),
                Arguments.of(exception, new LambdaEqualsMatcher<>("UNKNOWN", (p, r) -> r.getErrorType().getTemporality())),

                Arguments.of(eventHandlerExceptionInformation, new LambdaEqualsMatcher<>("java.lang.Exception: test", (p, r) -> p.getErrorMessage())),
                Arguments.of(eventHandlerExceptionInformation, new LambdaEqualsMatcher<>("testDescription", (p, r) -> p.getErrorDescription())),
                Arguments.of(exception, new LambdaStartWithMatcher("ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException: java.lang.Exception: test", (p, r) -> p.getStackTrace())),
                Arguments.of(eventHandlerExceptionInformation, new LambdaEqualsMatcher<>("TEST", (p, r) -> r.getErrorType().getCode())),
                Arguments.of(eventHandlerExceptionInformation, new LambdaEqualsMatcher<>("PERMANENT", (p, r) -> r.getErrorType().getTemporality())),
        };
        return Arrays.stream(matchers);
    }

    @BeforeEach
    void init(@Mock KafkaProperties kafkaProperties) {
        when(key.getSerializedMessage()).thenReturn(new byte[]{0, 1});
        when(value.getSerializedMessage()).thenReturn(new byte[]{1, 1});
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        when(kafkaProperties.getServiceName()).thenReturn("service");
        when(kafkaProperties.getSystemName()).thenReturn("system");
        when(kafkaProperties.getErrorTopicName(KafkaProperties.DEFAULT_CLUSTER)).thenReturn("errorTopic");
        when(kafkaProperties.getErrorEventStackTraceMaxLength()).thenReturn(500);
        doReturn(completableFuture).when(kafkaTemplate).send(eq("errorTopic"), any(), any());
        ExponentialBackOff retryErrorHandlingService = new ExponentialBackOff();
        doReturn(kafkaTemplate).when(beanFactory).getBean("kafkaTemplate");
        StackTraceHasher stackTraceHasher = new StackTraceHasher(kafkaProperties);
        this.target = new ErrorServiceSender(beanFactory, kafkaProperties, null, retryErrorHandlingService, null, stackTraceHasher);
    }

    @Test
    void sendToErrorTopic() {
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        Exception exception = new Exception();
        target.accept(record, exception);
        verify(kafkaTemplate, only()).send(eq("errorTopic"), keyCaptor.capture(), eventCaptor.capture());

        AvroDomainEvent event = eventCaptor.getValue();
        MessageProcessingFailedMessageKey key = keyCaptor.getValue();

        assertEquals(event.getIdentity().getIdempotenceId(), key.getKey());
    }

    @Test
    void sendToErrorTopic_avroMessageRecord_expectKeyToBeEqualToTheCausingEventId() {
        String causingeventId = "the-id";
        AvroDomainEvent domainEvent = Mockito.mock(AvroDomainEvent.class);
        AvroDomainEventIdentity domainEventIdentity = Mockito.mock(AvroDomainEventIdentity.class);
        doReturn(domainEventIdentity).when(domainEvent).getIdentity();
        doReturn(causingeventId).when(domainEventIdentity).getId();
        doReturn(new byte[]{1, 1}).when(domainEvent).getSerializedMessage();
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, domainEvent);
        Exception exception = new Exception();
        target.accept(record, exception);
        verify(kafkaTemplate, only()).send(eq("errorTopic"), keyCaptor.capture(), any());

        MessageProcessingFailedMessageKey key = keyCaptor.getValue();

        assertEquals(causingeventId, key.getKey());
    }

    @Test
    void noErrorDescription() {
        MessageHandlerException eventHandleException = MessageHandlerException.builder()
                .cause(new Exception("test"))
                .errorCode("TEST")
                .temporality(MessageHandlerExceptionInformation.Temporality.PERMANENT)
                .build();
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, eventHandleException);
        verify(kafkaTemplate, only()).send(any(), any(),
                ArgumentMatchers.argThat(new LambdaArgumentMatcher((p, r) -> p.getErrorDescription() == null, "Error Description must be null")));
    }

    @Test
    void accept_messageWithoutErrorCode_messageSentToErrorTopic() {
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, new ListenerExecutionFailedException("message", new NoErrorCodeException(null, MessageHandlerExceptionInformation.Temporality.PERMANENT)));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }

    @Test
    void accept_messageWithoutTemporality_messageSentToErrorTopic() {
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, new ListenerExecutionFailedException("message", new NoErrorCodeException("errorCode", null)));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }

    @Test
    void accept_messageWithoutErrorCodeAndTemporality_messageSentToErrorTopic() {
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, new ListenerExecutionFailedException("message", new NoErrorCodeException(null, null)));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }

    @Test
    void accept_messageWithoutMessage_messageSentToErrorTopic() {
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, new ListenerExecutionFailedException(null, new NoErrorCodeException("errorCode", MessageHandlerExceptionInformation.Temporality.PERMANENT)));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }

    @Test
    void accept_messageWithoutMessageAndCause_messageSentToErrorTopic() {
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, new ListenerExecutionFailedException(null, null));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }

    @ParameterizedTest
    @MethodSource("checkMessageSource")
    void checkMessage(Exception exception, ArgumentMatcher<MessageProcessingFailedEvent> matcher) {
        ListenerExecutionFailedException listenerException = new ListenerExecutionFailedException("test", exception);
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, value);
        target.accept(record, listenerException);
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), ArgumentMatchers.argThat(matcher));
    }

    @RequiredArgsConstructor
    private static class LambdaNullMatcher<T> implements ArgumentMatcher<MessageProcessingFailedEvent> {
        private final BiFunction<MessageProcessingFailedPayload, MessageProcessingFailedReferences, T> valueFunction;
        private String message;

        @Override
        public boolean matches(MessageProcessingFailedEvent argument) {
            T value = valueFunction.apply(argument.getOptionalPayload().orElse(null), argument.getReferences());
            message = value + " must be null";
            return value == null;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    @RequiredArgsConstructor
    private static class LambdaStartWithMatcher implements ArgumentMatcher<MessageProcessingFailedEvent> {
        private final String prefix;
        private final BiFunction<MessageProcessingFailedPayload, MessageProcessingFailedReferences, String> valueFunction;
        private String message;

        @Override
        public boolean matches(MessageProcessingFailedEvent argument) {
            String value = valueFunction.apply(argument.getOptionalPayload().orElse(null), argument.getReferences());
            message = value + " must start with " + prefix;
            return value.startsWith(prefix);
        }

        @Override
        public String toString() {
            return message;
        }
    }

    @RequiredArgsConstructor
    private static class LambdaByteArrayEqualsMatcher implements ArgumentMatcher<MessageProcessingFailedEvent> {
        private final byte[] expected;
        private final BiFunction<MessageProcessingFailedPayload, MessageProcessingFailedReferences, byte[]> valueFunction;
        private String message;

        @Override
        public boolean matches(MessageProcessingFailedEvent argument) {
            byte[] value = valueFunction.apply(argument.getOptionalPayload().orElse(null), argument.getReferences());
            message = Arrays.toString(value) + " equals " + Arrays.toString(expected);
            return Arrays.equals(expected, value);
        }

        @Override
        public String toString() {
            return message;
        }
    }

    @RequiredArgsConstructor
    private static class LambdaEqualsMatcher<T> implements ArgumentMatcher<MessageProcessingFailedEvent> {
        private final T expected;
        private final BiFunction<MessageProcessingFailedPayload, MessageProcessingFailedReferences, T> valueFunction;
        private String message;

        @Override
        public boolean matches(MessageProcessingFailedEvent argument) {
            T value = valueFunction.apply(argument.getOptionalPayload().orElse(null), argument.getReferences());
            message = value + " equals " + expected;
            return expected.equals(value);
        }

        @Override
        public String toString() {
            return message;
        }
    }

    @RequiredArgsConstructor
    private static class LambdaArgumentMatcher implements ArgumentMatcher<MessageProcessingFailedEvent> {
        private final BiFunction<MessageProcessingFailedPayload, MessageProcessingFailedReferences, Boolean> valueFunction;
        private final String message;

        @Override
        public boolean matches(MessageProcessingFailedEvent argument) {
            return valueFunction.apply(argument.getOptionalPayload().orElse(null), argument.getReferences());
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
