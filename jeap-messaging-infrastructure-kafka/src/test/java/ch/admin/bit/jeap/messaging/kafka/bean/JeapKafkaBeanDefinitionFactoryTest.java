package ch.admin.bit.jeap.messaging.kafka.bean;

import ch.admin.bit.jeap.messaging.kafka.filter.ErrorHandlingTargetFilter;
import ch.admin.bit.jeap.messaging.kafka.interceptor.CallbackRecordInterceptor;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CompositeRecordInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.stream.Stream;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JeapKafkaBeanDefinitionFactoryTest {

    public static final String CLUSTER_NAME = "defaultCluster";
    @Mock
    private BeanFactory beanFactory;
    @Mock
    private KafkaProperties springKafkaProperties;
    @Mock
    private JeapKafkaBeanNames jeapKafkaBeanNames;
    @Mock
    private ErrorHandlingTargetFilter errorHandlingTargetFilter;
    @Mock
    private ObjectProvider<RecordInterceptor> recordInterceptorProvider;
    @Mock
    private ObjectProvider<JeapKafkaMessageCallback> kafkaMessageCallbackObjectProvider;

    private JeapKafkaBeanDefinitionFactory factory;


    @BeforeEach
    void setUp() {
        ConsumerFactory<Object, Object> kafkaConsumerFactory = mock(ConsumerFactory.class);
        KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);

        when(jeapKafkaBeanNames.getConsumerFactoryBeanName(CLUSTER_NAME)).thenReturn("myConsumerFactoryBeanName");
        when(beanFactory.getBean("myConsumerFactoryBeanName")).thenReturn(kafkaConsumerFactory);
        when(jeapKafkaBeanNames.getKafkaTemplateBeanName(CLUSTER_NAME)).thenReturn("myKafkaTemplateBeanName");
        when(beanFactory.getBean("myKafkaTemplateBeanName")).thenReturn(kafkaTemplate);
        when(jeapKafkaBeanNames.getAdminBeanName(CLUSTER_NAME)).thenReturn("myKafkaAdminBeanName");
        when(beanFactory.getBean("myKafkaAdminBeanName")).thenReturn(kafkaAdmin);
        when(jeapKafkaBeanNames.getProducerFactoryBeanName(CLUSTER_NAME)).thenReturn("myProducerFactoryBeanName");

        ConcurrentKafkaListenerContainerFactoryConfigurer listenerContainerFactoryConfigurer = mock(ConcurrentKafkaListenerContainerFactoryConfigurer.class);
        when(beanFactory.getBean("kafkaListenerContainerFactoryConfigurer")).thenReturn(listenerContainerFactoryConfigurer);

        when(beanFactory.getBeanProvider(RecordInterceptor.class)).thenReturn(recordInterceptorProvider);
        when(recordInterceptorProvider.stream()).thenReturn(Stream.empty());

        when(beanFactory.getBeanProvider(JeapKafkaMessageCallback.class)).thenReturn(kafkaMessageCallbackObjectProvider);
        when(kafkaMessageCallbackObjectProvider.stream()).thenReturn(Stream.empty());

        ObjectProvider<KafkaTransactionManager> kafkaTransactionManagerkafkaTransactionManagerObjectProvider = mock(ObjectProvider.class);
        when(beanFactory.getBeanProvider(KafkaTransactionManager.class)).thenReturn(kafkaTransactionManagerkafkaTransactionManagerObjectProvider);

        ObjectProvider<ContainerCustomizer> containerCustomizer = mock(ObjectProvider.class);
        when(beanFactory.getBeanProvider(ContainerCustomizer.class)).thenReturn(containerCustomizer);

        ObjectProvider<TracerBridge> tracerBridgeObjectProvider = mock(ObjectProvider.class);
        when(beanFactory.getBeanProvider(TracerBridge.class)).thenReturn(tracerBridgeObjectProvider);

        when(beanFactory.getBean(ErrorHandlingTargetFilter.class)).thenReturn(errorHandlingTargetFilter);

        factory = new JeapKafkaBeanDefinitionFactory(beanFactory, springKafkaProperties, jeapKafkaBeanNames, CLUSTER_NAME);
    }

    @Test
    void createListenerContainerFactory_validateRegisteringErrorHandlingTargetFilter() {
        GenericBeanDefinition genericBeanDefinition = factory.createListenerContainerFactory(CLUSTER_NAME);
        Supplier<?> instanceSupplier = genericBeanDefinition.getInstanceSupplier();
        Object object = instanceSupplier.get();
        assertNotNull(object);
        assertTrue(object instanceof ConcurrentKafkaListenerContainerFactory<?, ?>);
        ConcurrentKafkaListenerContainerFactory<?, ?> factoryInstance = (ConcurrentKafkaListenerContainerFactory<?, ?>) object;

        Object recordFilterStrategy = ReflectionTestUtils.getField(factoryInstance, "recordFilterStrategy");
        assertEquals(errorHandlingTargetFilter, recordFilterStrategy);

        Object ackDiscarded = ReflectionTestUtils.getField(factoryInstance, "ackDiscarded");
        assertTrue((Boolean) ackDiscarded);
    }

    @Test
    void createListenerContainerFactory_setsSingleRecordInterceptor_whenOnlyCallbacksExist() {
        JeapKafkaMessageCallback callback = mock(JeapKafkaMessageCallback.class);
        when(kafkaMessageCallbackObjectProvider.stream()).thenReturn(Stream.of(callback));

        GenericBeanDefinition genericBeanDefinition = factory.createListenerContainerFactory(CLUSTER_NAME);
        ConcurrentKafkaListenerContainerFactory<?, ?> factoryInstance =
                (ConcurrentKafkaListenerContainerFactory<?, ?>) genericBeanDefinition.getInstanceSupplier().get();

        Object recordInterceptor = ReflectionTestUtils.getField(factoryInstance, "recordInterceptor");
        assertNotNull(recordInterceptor);
        assertTrue(recordInterceptor instanceof CallbackRecordInterceptor);
    }

    @Test
    void createListenerContainerFactory_setsCompositeRecordInterceptor_whenMultipleInterceptorsExist() {
        RecordInterceptor<?, ?> existingRecordInterceptor = mock(RecordInterceptor.class);
        JeapKafkaMessageCallback callback = mock(JeapKafkaMessageCallback.class);
        when(recordInterceptorProvider.stream()).thenReturn(Stream.of(existingRecordInterceptor));
        when(kafkaMessageCallbackObjectProvider.stream()).thenReturn(Stream.of(callback));

        GenericBeanDefinition genericBeanDefinition = factory.createListenerContainerFactory(CLUSTER_NAME);
        ConcurrentKafkaListenerContainerFactory<?, ?> factoryInstance =
                (ConcurrentKafkaListenerContainerFactory<?, ?>) genericBeanDefinition.getInstanceSupplier().get();

        Object recordInterceptor = ReflectionTestUtils.getField(factoryInstance, "recordInterceptor");
        assertNotNull(recordInterceptor);
        assertTrue(recordInterceptor instanceof CompositeRecordInterceptor);
    }

    @Test
    void createKafkaTemplate_setsDefaultTopicAndTransactionIdPrefix_whenConfigured() {
        KafkaProperties.Template templateProperties = mock(KafkaProperties.Template.class);
        when(springKafkaProperties.getTemplate()).thenReturn(templateProperties);
        when(templateProperties.getDefaultTopic()).thenReturn("orders.v1");
        when(templateProperties.getTransactionIdPrefix()).thenReturn("tx-orders-");

        GenericBeanDefinition beanDefinition = factory.createKafkaTemplate(CLUSTER_NAME);
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        assertEquals("orders.v1", propertyValues.get("defaultTopic"));
        assertEquals("tx-orders-", propertyValues.get("transactionIdPrefix"));
    }

    @Test
    void createKafkaTemplate_doesNotSetDefaultTopicAndTransactionIdPrefix_whenNotConfigured() {
        KafkaProperties.Template templateProperties = mock(KafkaProperties.Template.class);
        when(springKafkaProperties.getTemplate()).thenReturn(templateProperties);
        when(templateProperties.getDefaultTopic()).thenReturn(null);
        when(templateProperties.getTransactionIdPrefix()).thenReturn(null);

        GenericBeanDefinition beanDefinition = factory.createKafkaTemplate(CLUSTER_NAME);
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        assertNull(propertyValues.getPropertyValue("defaultTopic"));
        assertNull(propertyValues.getPropertyValue("transactionIdPrefix"));
    }

}
