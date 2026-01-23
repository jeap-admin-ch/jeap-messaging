package ch.admin.bit.jeap.messaging.kafka.bean;

import ch.admin.bit.jeap.messaging.kafka.filter.ErrorHandlingTargetFilter;
import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.messaging.kafka.spring.JeapKafkaBeanNames;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

        ConcurrentKafkaListenerContainerFactoryConfigurer listenerContainerFactoryConfigurer = mock(ConcurrentKafkaListenerContainerFactoryConfigurer.class);
        when(beanFactory.getBean("kafkaListenerContainerFactoryConfigurer")).thenReturn(listenerContainerFactoryConfigurer);

        ObjectProvider<RecordInterceptor> recordInterceptorProvider = mock(ObjectProvider.class);
        when(beanFactory.getBeanProvider(RecordInterceptor.class)).thenReturn(recordInterceptorProvider);

        ObjectProvider<JeapKafkaMessageCallback> kafkaMessageCallbackObjectProvider = mock(ObjectProvider.class);
        when(beanFactory.getBeanProvider(JeapKafkaMessageCallback.class)).thenReturn(kafkaMessageCallbackObjectProvider);

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

}
