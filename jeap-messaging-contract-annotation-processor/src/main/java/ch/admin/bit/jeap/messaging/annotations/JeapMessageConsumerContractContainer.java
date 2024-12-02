package ch.admin.bit.jeap.messaging.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This is the repeatable annotation container for JeapMessageConsumerContract
 */
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface JeapMessageConsumerContractContainer {
    JeapMessageConsumerContract[] value();
}
