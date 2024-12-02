package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StackTraceHasherTest {

    private static final String STACK_TRACE_STRING = """
            ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException: java.util.NoSuchElementException: No value present
                at ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException$MessageHandlerExceptionBuilder.build(MessageHandlerException.java:36)
                at ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceSender.convertToValidExceptionInformation(ErrorServiceSender.java:242)
                at org.springframework.kafka.listener.FailedRecordTracker.lambda$new$1(FailedRecordTracker.java:97)
                at org.springframework.kafka.listener.FailedRecordTracker.attemptRecovery(FailedRecordTracker.java:228)
                at org.springframework.kafka.listener.SeekUtils.lambda$doSeeks$5(SeekUtils.java:108)
                at java.base/java.util.ArrayList.forEach(Unknown Source)
                at org.springframework.kafka.listener.DefaultErrorHandler.handleRemaining(DefaultErrorHandler.java:168)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.invokeErrorHandler(KafkaMessageListenerContainer.java:2836)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.lambda$doInvokeRecordListener$53(KafkaMessageListenerContainer.java:2713)
                at io.micrometer.observation.Observation.observe(Observation.java:565)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.doInvokeRecordListener(KafkaMessageListenerContainer.java:2699)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.run(KafkaMessageListenerContainer.java:1296)
                at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(Unknown Source)
                at java.base/java.lang.Thread.run(Unknown Source)
            Caused by: java.util.NoSuchElementException: No value present
                at java.base/java.util.Optional.orElseThrow(Unknown Source)
                at ch.admin.bit.jeap.jme.processcontext.domain.TestProcessService.processCompleted(TestProcessService.java:57)
                at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)
                at java.base/java.lang.reflect.Method.invoke(Unknown Source)
                at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:355)
                at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
                at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
                at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
                at ch.admin.bit.jeap.jme.processcontext.domain.TestProcessService$$SpringCGLIB$$0.processCompleted(<generated>)
                at ch.admin.bit.jeap.jme.processcontext.kafka.EventConsumer.consumeProcessCompletedEvent(EventConsumer.java:45)
                at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Unknown Source)
                at java.base/java.lang.reflect.Method.invoke(Unknown Source)
                ... 10 more""";

    final String STACK_TRACE_STRING_PRE_FILTERED = """
            ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException: java.util.NoSuchElementException: No value present
                at ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerException$MessageHandlerExceptionBuilder.build(MessageHandlerException.java:36)
                at ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceSender.convertToValidExceptionInformation(ErrorServiceSender.java:242)
                at org.springframework.kafka.listener.FailedRecordTracker.lambda$new$1(FailedRecordTracker.java:97)
                at org.springframework.kafka.listener.FailedRecordTracker.attemptRecovery(FailedRecordTracker.java:228)
                at org.springframework.kafka.listener.SeekUtils.lambda$doSeeks$5(SeekUtils.java:108)
                at org.springframework.kafka.listener.DefaultErrorHandler.handleRemaining(DefaultErrorHandler.java:168)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.invokeErrorHandler(KafkaMessageListenerContainer.java:2836)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.lambda$doInvokeRecordListener$53(KafkaMessageListenerContainer.java:2713)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.doInvokeRecordListener(KafkaMessageListenerContainer.java:2699)
                at org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer.run(KafkaMessageListenerContainer.java:1296)
            Caused by: java.util.NoSuchElementException: No value present
                at ch.admin.bit.jeap.jme.processcontext.domain.TestProcessService.processCompleted(TestProcessService.java:57)
                at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
                at ch.admin.bit.jeap.jme.processcontext.kafka.EventConsumer.consumeProcessCompletedEvent(EventConsumer.java:45)
                ... 10 more""";

    private StackTraceHasher stackTraceHasher;
    private StackTraceHasher stackTraceHasherNoFiltering;

    @BeforeEach
    void setUp() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setErrorStackTraceHashAdditionalExclusionPatterns(List.of("^io\\.micrometer\\..+"));
        stackTraceHasher = new StackTraceHasher(kafkaProperties);

        KafkaProperties kafkaPropertiesNoFiltering = new KafkaProperties();
        kafkaPropertiesNoFiltering.setErrorStackTraceHashDefaultExclusionPatterns(List.of());
        kafkaPropertiesNoFiltering.setErrorStackTraceHashAdditionalExclusionPatterns(List.of());
        stackTraceHasherNoFiltering = new StackTraceHasher(kafkaPropertiesNoFiltering);
    }

    @Test
    void testHash_WhenTheSameThrowable_ThenTheSameHash() {
        final Throwable throwable = createThrowableFromStackTraceString(STACK_TRACE_STRING);

        final String hash = stackTraceHasher.hash(throwable);
        final String hashAgain = stackTraceHasher.hash(throwable);

        assertThat(hash).isEqualTo(hashAgain);
    }

    @Test
    void testHash_WhenTheSameStackTrace_ThenTheSameHash() {
        final Throwable throwable = createThrowableFromStackTraceString(STACK_TRACE_STRING);
        final Throwable throwableSameStackTrace = createThrowableFromStackTraceString(STACK_TRACE_STRING);

        final String hash = stackTraceHasher.hash(throwable);
        final String hashSameStackTrace = stackTraceHasher.hash(throwableSameStackTrace);

        assertThat(hash).isEqualTo(hashSameStackTrace);
    }

    @Test
    void testHash_WhenTheDifferentStackTrace_ThenDifferentHash() {
        final String differentStackTraceString = STACK_TRACE_STRING.replaceAll("jeap", "test");
        final Throwable throwable = createThrowableFromStackTraceString(STACK_TRACE_STRING);
        final Throwable throwableDifferentStackTrace = createThrowableFromStackTraceString(differentStackTraceString);

        final String hash = stackTraceHasher.hash(throwable);
        final String hashDifferentStackTrace = stackTraceHasher.hash(throwableDifferentStackTrace);

        assertThat(hash).isNotEqualTo(hashDifferentStackTrace);
    }

    @Test
    void testHash_WhenExclusionsConfigured_ThenDifferentHash() {
        final Throwable throwable = createThrowableFromStackTraceString(STACK_TRACE_STRING);

        final String hash = stackTraceHasher.hash(throwable);
        final String hashNoFiltering = stackTraceHasherNoFiltering.hash(throwable);

        assertThat(hash).isNotEqualTo(hashNoFiltering);
    }

    @Test
    void testHash_WhenExclusionsConfigured_ThenExcludedLinesDontAddToHash() {
        final Throwable throwableFullStackTrace = createThrowableFromStackTraceString(STACK_TRACE_STRING);
        final Throwable throwablePrefilteredStackTrace = createThrowableFromStackTraceString(STACK_TRACE_STRING_PRE_FILTERED);

        final String hashFilteringFullStackTrace = stackTraceHasher.hash(throwableFullStackTrace);
        final String hashNoFilteringPrefilteredStackTrace = stackTraceHasherNoFiltering.hash(throwablePrefilteredStackTrace);

        assertThat(hashFilteringFullStackTrace).isEqualTo(hashNoFilteringPrefilteredStackTrace);
    }

    private Throwable createThrowableFromStackTraceString(String stackTraceString) {
        return new FixedStackTraceThrowable(StackTraceParser.parseStackTrace(stackTraceString));
    }

    @RequiredArgsConstructor
    private static class FixedStackTraceThrowable extends Throwable {
        private final StackTraceElement[] stackTraceElements;
        @Override
        public StackTraceElement[] getStackTrace() {
            return stackTraceElements;
        }
    }

}
