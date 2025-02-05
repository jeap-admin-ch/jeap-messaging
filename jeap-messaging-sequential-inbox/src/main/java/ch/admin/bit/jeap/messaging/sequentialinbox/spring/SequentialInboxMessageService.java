package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer.SequentialInboxConfigurationLoader;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.kafka.KafkaSequentialInboxMessageConsumerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Component
@Slf4j
class SequentialInboxMessageService {
    private final KafkaSequentialInboxMessageConsumerFactory messageConsumerFactory;
    private final SequentialInboxConfigurationLoader sequentialInboxConfigurationLoader;
    private final ApplicationContext applicationContext;

    public SequentialInboxMessageService(KafkaSequentialInboxMessageConsumerFactory messageConsumerFactory, ApplicationContext applicationContext) {
        this.messageConsumerFactory = messageConsumerFactory;
        this.applicationContext = applicationContext;
        this.sequentialInboxConfigurationLoader = new SequentialInboxConfigurationLoader();
    }

    @EventListener
    @SuppressWarnings("unused")
    public void onAppStarted(ApplicationStartedEvent event) {
        startDomainEventListeners();
    }

    void startDomainEventListeners() {

        SequentialInboxConfiguration sequentialInboxConfiguration = sequentialInboxConfigurationLoader.loadSequenceDeclaration();

        Set<SequencedMessageType> messageTypes = sequentialInboxConfiguration.getSequences().stream()
                .flatMap(sequence -> sequence.getMessages().stream())
                .collect(toSet());

        List<Object> allAnnotatedBeans = getAllAnnotatedBeans();

        messageTypes.forEach(messageType -> messageConsumerFactory.startConsumer(messageType.getTopic(), messageType.getType(), messageType.getClusterName(), getBeanForMessageType(allAnnotatedBeans, messageType.getType())));
    }

    private SequentialInboxMessageHandler getBeanForMessageType(List<Object> beans, String messageType) {
        List<SequentialInboxMessageHandler> beansForMessageType = new ArrayList<>();
        for (Object bean : beans) {
            List<Method> list = Arrays.stream(AopUtils.getTargetClass(bean).getMethods()).filter(method -> method.isAnnotationPresent(SequentialInboxMessageListener.class)).toList();
            for (Method method : list) {
                if (method.getParameterTypes().length == 2 &&
                        method.getParameterTypes()[0].getName().equals(messageType) &&
                        method.getParameterTypes()[1].isAssignableFrom(Acknowledgment.class)) {
                    log.info("Found message handler {} for message type {}", bean.getClass().getName(), messageType);
                    beansForMessageType.add(new SequentialInboxMessageHandler(bean, method));
                }
            }
        }

        if (beansForMessageType.isEmpty()) {
            throw SequentialInboxException.noMessageHandlerFound(messageType);
        }
        if (beansForMessageType.size() != 1) {
            throw SequentialInboxException.multipleMessageHandlersFound(messageType, beansForMessageType);
        }

        return beansForMessageType.getFirst();
    }

    private List<Object> getAllAnnotatedBeans() {
        return Arrays.stream(applicationContext.getBeanDefinitionNames()).map(applicationContext::getBean).toList().stream().filter(bean ->
                !Arrays.stream(AopUtils.getTargetClass(bean).getMethods()).filter(method -> method.isAnnotationPresent(SequentialInboxMessageListener.class)).toList().isEmpty()).toList();
    }

}
