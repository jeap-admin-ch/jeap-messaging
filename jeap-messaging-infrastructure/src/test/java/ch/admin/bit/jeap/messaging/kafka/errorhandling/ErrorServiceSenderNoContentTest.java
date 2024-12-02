package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.avro.SerializedMessageHolder;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorServiceSenderNoContentTest {
    @Mock
    private KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;
    @Mock
    private BeanFactory beanFactory;
    @Mock
    private SerializedMessageHolder key;
    @Mock
    private SerializedMessageHolder value;
    @Mock
    private CompletableFuture<SendResult<Void, MessageProcessingFailedEvent>> completableFuture;

    private ErrorServiceSender target;

    @BeforeEach
    void init(@Mock KafkaProperties kafkaProperties) {
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        when(kafkaProperties.getServiceName()).thenReturn("service");
        when(kafkaProperties.getSystemName()).thenReturn("system");
        when(kafkaProperties.getErrorTopicName(KafkaProperties.DEFAULT_CLUSTER)).thenReturn("errorTopic");
        doReturn(completableFuture).when(kafkaTemplate).send(eq("errorTopic"), any(), any());
        doReturn(kafkaTemplate).when(beanFactory).getBean("kafkaTemplate");
        ExponentialBackOff retryErrorHandlingService = new ExponentialBackOff();
        StackTraceHasher stackTraceHasher = new StackTraceHasher(kafkaProperties);
        this.target = new ErrorServiceSender(beanFactory, kafkaProperties, null, retryErrorHandlingService, null, stackTraceHasher);
    }

    @Test
    void accept_messageWithoutPayload_messageSentToErrorTopic() {
        when(key.getSerializedMessage()).thenReturn(new byte[]{0, 1});
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, key, null);
        target.accept(record, new ListenerExecutionFailedException(null, null));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }

    @Test
    void accept_messageWithoutKey_messageSentToErrorTopic() {
        when(value.getSerializedMessage()).thenReturn(new byte[]{0, 1});
        ConsumerRecord<?, ?> record = new ConsumerRecord<>("topic", 1, 3, null, value);
        target.accept(record, new ListenerExecutionFailedException(null, null));
        verify(kafkaTemplate, only()).send(eq("errorTopic"), any(), any());
    }
}
