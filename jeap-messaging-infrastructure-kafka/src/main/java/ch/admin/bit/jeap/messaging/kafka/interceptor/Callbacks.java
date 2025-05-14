package ch.admin.bit.jeap.messaging.kafka.interceptor;

import ch.admin.bit.jeap.messaging.model.Message;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@UtilityClass
@Slf4j
public class Callbacks {

    public void invokeCallback(Message msg, String topicName, BiConsumer<Message, String> method) {
        try {
            method.accept(msg, topicName);
        } catch (Exception e) {
            log.warn("Exception in callback", e);
        }
    }
}
