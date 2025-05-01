package ch.admin.bit.jeap.messaging.kafka.interceptor;

import ch.admin.bit.jeap.messaging.model.Message;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@UtilityClass
@Slf4j
public class Callbacks {

    public void invokeCallback(Message msg, Consumer<Message> method) {
        try {
            method.accept(msg);
        } catch (Exception e) {
            log.warn("Exception in callback", e);
        }
    }
}
