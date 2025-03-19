package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequencedMessageType;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.kafka.KafkaSequentialInboxMessageConsumerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Component
@Slf4j
class SequentialInboxListenerService {
    private final KafkaSequentialInboxMessageConsumerFactory messageConsumerFactory;
    private final ApplicationContext applicationContext;
    private final SequentialInboxConfiguration sequentialInboxConfiguration;
    private final MessageHandlerProvider messageHandlerProvider;

    public SequentialInboxListenerService(KafkaSequentialInboxMessageConsumerFactory messageConsumerFactory, ApplicationContext applicationContext, SequentialInboxConfiguration sequentialInboxConfiguration, MessageHandlerProvider messageHandlerProvider) {
        this.messageConsumerFactory = messageConsumerFactory;
        this.applicationContext = applicationContext;
        this.sequentialInboxConfiguration = sequentialInboxConfiguration;
        this.messageHandlerProvider = messageHandlerProvider;
    }

    @EventListener
    public void onAppStarted(ApplicationStartedEvent ignored) {
        startMessageListeners();
    }

    void startMessageListeners() {
        Map<String, List<SequencedMessageType>> sequencedTypesByJeapMessageTypeName = sequentialInboxConfiguration.getSequencedMessageTypes()
                .stream()
                .collect(groupingBy(SequencedMessageType::getJeapMessageTypeName));
        List<SequencedMessageType> sequencedMessageTypeConfigsOncePerJeapMessageType =
                sequencedTypesByJeapMessageTypeName.values().stream()
                        .map(List::getFirst)
                        .toList();

        List<ListenerBeanMethod> allAnnotatedMethods = getAllSequentialInboxMessageListenerMethods();

        Set<ListenerBeanMethod> startedListeners = sequencedMessageTypeConfigsOncePerJeapMessageType.stream()
                .map(messageType -> startListener(messageType.getJeapMessageTypeName(), messageType.getTopic(), messageType.getClusterName(), allAnnotatedMethods))
                .collect(toSet());

        assertAllAnnotatedListenersStarted(allAnnotatedMethods, startedListeners);
    }

    private static void assertAllAnnotatedListenersStarted(List<ListenerBeanMethod> allAnnotatedMethods, Set<ListenerBeanMethod> startedListeners) {
        Set<ListenerBeanMethod> notStartedListeners = new HashSet<>(allAnnotatedMethods);
        notStartedListeners.removeAll(startedListeners);
        if (!notStartedListeners.isEmpty()) {
            throw SequentialInboxException.unusedMessageHandlers(notStartedListeners);
        }
    }

    private ListenerBeanMethod startListener(String jeapMessageTypeName, String topic, String clusterName, List<ListenerBeanMethod> allAnnotatedMethods) {
        SequentialInboxMessageHandler messageHandler = getBeanForMessageType(allAnnotatedMethods, jeapMessageTypeName);
        messageHandlerProvider.addHandler(jeapMessageTypeName, messageHandler);
        messageConsumerFactory.startConsumer(topic, jeapMessageTypeName, clusterName, messageHandler);
        return messageHandler.getListenerBeanMethod();
    }

    private SequentialInboxMessageHandler getBeanForMessageType(List<ListenerBeanMethod> allAnnotatedMethods, String messageType) {
        List<SequentialInboxMessageHandler> beansForMessageType = new ArrayList<>();
        for (ListenerBeanMethod listenerBeanMethod : allAnnotatedMethods) {
            Object bean = listenerBeanMethod.bean();
            Method method = listenerBeanMethod.method();
            int paramCount = method.getParameterTypes().length;
            validateListenerMethodSignature(method, paramCount);
            if (paramCount == 1 && getParameterType(method, 0).getSimpleName().equals(messageType)) {
                log.info("Found message handler {} for message type {}", bean.getClass().getName(), messageType);
                beansForMessageType.add(new SequentialInboxMessageHandler(listenerBeanMethod, false));
            }
            if (paramCount == 2 && method.getParameterTypes()[1].getSimpleName().equals(messageType)) {
                log.info("Found message handler {} with key for message type {}", bean.getClass().getName(), messageType);
                beansForMessageType.add(new SequentialInboxMessageHandler(listenerBeanMethod, true));
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

    private void validateListenerMethodSignature(Method method, int paramCount) {
        assertParamCount(method, paramCount);
        if (paramCount == 1 && !AvroMessage.class.isAssignableFrom(getParameterType(method, 0))) {
            throw SequentialInboxException.invalidListenerMethodSignature(method);
        }
        if (paramCount == 2 && (!AvroMessageKey.class.isAssignableFrom(getParameterType(method, 0)) || !AvroMessage.class.isAssignableFrom(getParameterType(method, 1)))) {
            throw SequentialInboxException.invalidListenerMethodSignature(method);
        }
    }

    private static Class<?> getParameterType(Method method, int pos) {
        return method.getParameterTypes()[pos];
    }

    private List<ListenerBeanMethod> getAllSequentialInboxMessageListenerMethods() {
        return Arrays.stream(applicationContext.getBeanDefinitionNames())
                .map(applicationContext::getBean)
                .flatMap(SequentialInboxListenerService::getSequentialInboxMessageListenerMethods)
                .toList();
    }

    private static Stream<ListenerBeanMethod> getSequentialInboxMessageListenerMethods(Object bean) {
        return Arrays.stream(AopUtils.getTargetClass(bean).getMethods())
                .filter(method -> method.isAnnotationPresent(SequentialInboxMessageListener.class))
                .map(method -> new ListenerBeanMethod(method, bean, getMessageTypeClass(method)));
    }

    @SuppressWarnings("unchecked") // parameter types are validated in validateListenerMethodSignature
    private static Class<AvroMessage> getMessageTypeClass(Method method) {
        int paramCount = method.getParameterTypes().length;
        assertParamCount(method, paramCount);
        int lastParameterIndex = paramCount - 1; // last parameter is always the message type, see validateListenerMethodSignature
        return (Class<AvroMessage>) getParameterType(method, lastParameterIndex);
    }

    private static void assertParamCount(Method method, int paramCount) {
        if (paramCount < 1 || paramCount > 2) {
            throw SequentialInboxException.invalidListenerMethodSignature(method);
        }
    }
}
