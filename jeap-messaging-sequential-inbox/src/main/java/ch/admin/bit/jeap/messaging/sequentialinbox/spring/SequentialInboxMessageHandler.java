package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SequentialInboxMessageHandler {

    private final ListenerBeanMethod listenerBeanMethod;
    private final boolean methodHasKeyParameter;

    SequentialInboxMessageHandler(ListenerBeanMethod listenerBeanMethod, boolean methodHasKeyParameter) {
        this.listenerBeanMethod = listenerBeanMethod;
        this.methodHasKeyParameter = methodHasKeyParameter;
    }

    public void invoke(AvroMessageKey avroMessageKey, AvroMessage message) {
        Method method = listenerBeanMethod.method();
        try {
            invokeHandlerMethod(avroMessageKey, message, method);
        } catch (InvocationTargetException ite) {
            Throwable cause = throwCauseIfPossible(ite);
            throw SequentialInboxException.handlerMethodInvocationFailed(method, cause == null ? ite : cause);
        } catch (Exception e) {
            throw SequentialInboxException.handlerMethodReflectiveOperationFailed(method, e);
        }
    }

    /**
     * Throw the throwable originating from the handler method if it is
     * <ul>
     * <li>A {@link RuntimeException} that can be safely thrown up to the ErrorHandler</li>
     * <li>An {@link Error} that should not be swallowed</li>
     * </ul>
     * Otherwise, the cause is returned and must be wrapped in a {@link SequentialInboxException}.
     */
    private static Throwable throwCauseIfPossible(InvocationTargetException ite) {
        Throwable cause = ite.getCause();
        if (cause instanceof RuntimeException causeEx) {
            throw causeEx;
        }
        if (cause instanceof Error causeError) {
            throw causeError;
        }
        return cause;
    }

    private void invokeHandlerMethod(AvroMessageKey avroMessageKey, AvroMessage message, Method method) throws IllegalAccessException, InvocationTargetException {
        Object bean = listenerBeanMethod.bean();
        if (methodHasKeyParameter) {
            method.invoke(bean, avroMessageKey, message);
        } else {
            method.invoke(bean, message);
        }
    }

    ListenerBeanMethod getListenerBeanMethod() {
        return listenerBeanMethod;
    }

    public Class<AvroMessage> getMessageTypeClass() {
        return listenerBeanMethod.messageTypeClass();
    }

    @Override
    public String toString() {
        return listenerBeanMethod.bean().getClass().getName() + "#" + listenerBeanMethod.method().getName() + " key=" + methodHasKeyParameter;
    }
}
