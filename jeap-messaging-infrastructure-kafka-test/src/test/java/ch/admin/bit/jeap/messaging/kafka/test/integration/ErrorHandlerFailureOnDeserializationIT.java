package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.errorevent.MessageProcessingFailedEvent;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.CreateSerializedMessageHolder;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.serde.SerdeUtils;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.CustomKafkaAvroDeserializer;
import ch.admin.bit.jeap.messaging.kafka.serde.confluent.config.KafkaConfluentAvroSerdeProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaPropertyFactory;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaTestConstants;
import ch.admin.bit.jeap.messaging.kafka.test.TestMessageSender;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestCommandConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=ErrorHandlerCommandIT"})
@DirtiesContext
@Import({ErrorHandlerFailureOnDeserializationIT.TestDeserializationConfig.class,
        ErrorHandlerFailureOnDeserializationIT.TestBeanPostProcessor.class})
class ErrorHandlerFailureOnDeserializationIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<MessageProcessingFailedEvent> errorEventProcessor;

    @Test
    void preserveSignatureHeaders() {

        //Publish a normal command, listener is executed and an error is generated
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create().idempotenceId("idempotenceId").text("message").build();

        TestMessageSender.sendSyncWithHeaders(kafkaTemplate, TestCommandConsumer.TOPIC_NAME, null, message,
                KafkaTestConstants.TEST_PRODUCER_DISABLE_CONTRACT_CHECK_HEADER,
                new RecordHeader("dummy1", new byte[]{1}),
                new RecordHeader(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, new byte[]{1, 2, 3}),
                new RecordHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY, new byte[]{1, 2, 3, 4}),
                new RecordHeader(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY, new byte[]{1, 2, 3, 4, 5}),
                new RecordHeader("dummy2", new byte[]{1, 2, 3, 4, 5, 6, 7})
        );
        MessageProcessingFailedEvent resultingError = expectError();

        Assertions.assertNotNull(resultingError);
        Assertions.assertNotNull(resultingError.getPayload());
        Assertions.assertNotNull(resultingError.getPayload().getFailedMessageMetadata());
        Map<String, ByteBuffer> headers = resultingError.getPayload().getFailedMessageMetadata().getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(3, headers.size());
        Assertions.assertNotNull(headers.get(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY));
        Assertions.assertNotNull(headers.get(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY));
        Assertions.assertNotNull(headers.get(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY));
    }

    private MessageProcessingFailedEvent expectError() {
        ArgumentCaptor<MessageProcessingFailedEvent> captor = ArgumentCaptor.forClass(MessageProcessingFailedEvent.class);
        Mockito.verify(errorEventProcessor, Mockito.timeout(TimeUnit.SECONDS.toMillis(80))).receive(captor.capture());
        return captor.getValue();
    }

    @Configuration
    public static class TestDeserializationConfig {

        @Bean
        @Primary
        public Deserializer<Object> valueDeserializer(Environment environment) {
            KafkaProperties kafkaProperties = JeapKafkaPropertyFactory.createJeapKafkaProperties(environment);
            KafkaConfluentAvroSerdeProperties serdeProperties = new KafkaConfluentAvroSerdeProperties(kafkaProperties, null, null);

            Deserializer<Object> deserializer = new TestDeserializer();
            Deserializer<Object> deserializerWithErrorHandling = SerdeUtils.deserializerWithErrorHandling("default", deserializer, false, serdeProperties);

            TestErrorHandlingDeserializer errorHandlingValueDeserializer = new TestErrorHandlingDeserializer(deserializerWithErrorHandling);
            errorHandlingValueDeserializer.setFailedDeserializationFunction(new CreateSerializedMessageHolder());
            errorHandlingValueDeserializer.configure(serdeProperties.avroDeserializerProperties("default"), false);
            return errorHandlingValueDeserializer;
        }

    }

    public static class TestErrorHandlingDeserializer extends ErrorHandlingDeserializer<Object> {

        public TestErrorHandlingDeserializer(Deserializer deserializer) {
            super(deserializer);
        }

        public TestErrorHandlingDeserializer() {
            super();
        }

        @Override
        public void setupDelegate(Map<String, ?> configs, String configKey) {
            Map<String, ?> adaptedConfigs = adaptConfig(configs);
            super.setupDelegate(adaptedConfigs, configKey);
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            Map<String, ?> adaptedConfigs = adaptConfig(configs);
            super.configure(adaptedConfigs, isKey);
        }

        private Map<String, ?> adaptConfig(Map<String, ?> configs) {
            Map<String, Object> result = new HashMap<>(configs);
            result.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, TestDeserializer.class);
            return result;
        }
    }

    public static class TestDeserializer implements Deserializer<Object> {

        private Deserializer<Object> delegate;

        public TestDeserializer() {
            this.delegate = new CustomKafkaAvroDeserializer();
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            delegate.configure(configs, isKey);
        }

        @Override
        public Object deserialize(String topic, byte[] data) {
            if (Objects.equals(TestCommandConsumer.TOPIC_NAME, topic)) {
                throw new RuntimeException("Failed while deserializing");
            }
            return delegate.deserialize(topic, data);
        }

        @Override
        public Object deserialize(String topic, Headers headers, byte[] data) {
            if (Objects.equals(TestCommandConsumer.TOPIC_NAME, topic)) {
                throw new RuntimeException("Failed while deserializing");
            }
            return delegate.deserialize(topic, headers, data);
        }

        @Override
        public Object deserialize(String topic, Headers headers, ByteBuffer data) {
            if (Objects.equals(TestCommandConsumer.TOPIC_NAME, topic)) {
                throw new RuntimeException("Failed while deserializing");
            }
            return delegate.deserialize(topic, headers, data);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }


    @Component
    public static class TestBeanPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (bean instanceof DefaultKafkaConsumerFactory<?, ?> consumerFactory) {
                try {
                    Field field = bean.getClass().getDeclaredField("configs");
                    field.setAccessible(true);
                    Map<Object, Object> map = (Map<Object, Object>) field.get(consumerFactory);
                    map.put("value.deserializer", TestErrorHandlingDeserializer.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return bean;
        }
    }
}
