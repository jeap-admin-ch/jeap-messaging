package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;

import java.lang.reflect.Method;

record ListenerBeanMethod(Method method, Object bean, Class<AvroMessage> messageTypeClass) {

    @Override
    public String toString() {
        return method.toString();
    }
}
