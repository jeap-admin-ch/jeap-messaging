package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SequentialInboxMessageHandlerTest {

    private SequentialInboxMessageHandler sequentialInboxMessageHandler;
    @Mock
    private AvroMessageKey avroMessageKey;
    @Mock
    private AvroMessage avroMessage;

    private boolean targetMethodInvoked = false;
    private boolean targetMethodWithKeyInvoked = false;

    private class InvocationTestTarget {

        public void targetMethod(AvroMessage avroMessage) {
            targetMethodInvoked = true;
        }

        public void targetMethodThrowingException(AvroMessage avroMessage) {
            throw new RuntimeException("cause");
        }

        public void targetMethodThrowingCheckedException(AvroMessage avroMessage) throws Exception {
            throw new Exception("checked exception cause");
        }

        public void targetMethodWithKey(AvroMessageKey avroMessageKey, AvroMessage avroMessage) {
            targetMethodWithKeyInvoked = true;
        }
    }

    private static class DifferentClass {
        public void someMethod() {
        }
    }

    @Test
    void invoke_withValidParameters() throws Exception {
        Object bean = new InvocationTestTarget();
        Method method = bean.getClass().getMethod("targetMethod", AvroMessage.class);
        ListenerBeanMethod listenerBeanMethod = new ListenerBeanMethod(method, bean, AvroMessage.class);

        sequentialInboxMessageHandler = new SequentialInboxMessageHandler(listenerBeanMethod, false);
        sequentialInboxMessageHandler.invoke(null, avroMessage);

        assertThat(targetMethodInvoked).isTrue();
    }

    @Test
    void invoke_withValidParameters_withKey() throws Exception {
        Object bean = new InvocationTestTarget();
        Method method = bean.getClass().getMethod("targetMethodWithKey", AvroMessageKey.class, AvroMessage.class);
        ListenerBeanMethod listenerBeanMethod = new ListenerBeanMethod(method, bean, AvroMessage.class);

        sequentialInboxMessageHandler = new SequentialInboxMessageHandler(listenerBeanMethod, true);
        sequentialInboxMessageHandler.invoke(avroMessageKey, avroMessage);

        assertThat(targetMethodWithKeyInvoked).isTrue();
    }

    @Test
    void invoke_throws() throws Exception {
        Object bean = new InvocationTestTarget();
        Method method = bean.getClass().getMethod("targetMethodThrowingException", AvroMessage.class);
        ListenerBeanMethod listenerBeanMethod = new ListenerBeanMethod(method, bean, AvroMessage.class);

        sequentialInboxMessageHandler = new SequentialInboxMessageHandler(listenerBeanMethod, false);

        assertThatThrownBy(() -> sequentialInboxMessageHandler.invoke(avroMessageKey, avroMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("cause");
        ;
    }

    @Test
    void invoke_throwsCheckedException() throws Exception {
        Object bean = new InvocationTestTarget();
        Method method = bean.getClass().getMethod("targetMethodThrowingCheckedException", AvroMessage.class);
        ListenerBeanMethod listenerBeanMethod = new ListenerBeanMethod(method, bean, AvroMessage.class);

        sequentialInboxMessageHandler = new SequentialInboxMessageHandler(listenerBeanMethod, false);

        assertThatThrownBy(() -> sequentialInboxMessageHandler.invoke(avroMessageKey, avroMessage))
                .isInstanceOf(SequentialInboxException.class)
                .hasMessageContaining("Invocation of message handler method");
    }

    @Test
    void invoke_reflectionError() throws Exception {
        Object bean = new InvocationTestTarget();
        Method differentObjectMethod = DifferentClass.class.getMethod("someMethod");
        ListenerBeanMethod listenerBeanMethod = new ListenerBeanMethod(differentObjectMethod, bean, AvroMessage.class);

        sequentialInboxMessageHandler = new SequentialInboxMessageHandler(listenerBeanMethod, false);

        assertThatThrownBy(() -> sequentialInboxMessageHandler.invoke(avroMessageKey, avroMessage))
                .isInstanceOf(SequentialInboxException.class)
                .hasMessageContaining("Reflection error");
    }

    @Test
    void testToString() throws Exception {
        Object bean = new Object();
        Method method = bean.getClass().getMethod("equals", Object.class);
        SequentialInboxMessageHandler sequentialInboxMessageHandler =
                new SequentialInboxMessageHandler(new ListenerBeanMethod(method, bean, AvroMessage.class), false);

        assertThat(sequentialInboxMessageHandler.toString())
                .hasToString("java.lang.Object#equals key=false");
    }
}
