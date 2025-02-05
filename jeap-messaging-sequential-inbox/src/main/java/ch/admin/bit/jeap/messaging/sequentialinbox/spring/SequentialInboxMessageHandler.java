package ch.admin.bit.jeap.messaging.sequentialinbox.spring;

import java.lang.reflect.Method;

public record SequentialInboxMessageHandler(Object bean, Method method) {}