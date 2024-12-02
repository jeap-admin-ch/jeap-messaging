package ch.admin.bit.jeap.messaging.kafka.test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@link KafkaListener} to be used in integration tests. Consumer contract checking is disabled by default for
 * this listener. This is useful i.e. when an integration test is used as the consumer of a message produced by the
 * application. In this case, the test will simulate the receiver and won't have a contract to consume the message.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@KafkaListener
public @interface TestKafkaListener {

    @AliasFor(annotation = KafkaListener.class, attribute = "id")
    String id() default "";

    @AliasFor(annotation = KafkaListener.class, attribute = "groupId")
    String groupId() default "${spring.application.name:testapp}-test";

    @AliasFor(annotation = KafkaListener.class, attribute = "topics")
    String[] topics() default {};

    @AliasFor(annotation = KafkaListener.class, attribute = "containerFactory")
    String containerFactory() default "";

    @AliasFor(annotation = KafkaListener.class, attribute = "properties")
    String[] properties() default {
            KafkaTestConstants.TEST_CONSUMER_DISABLE_CONTRACT_CHECK_PROPERTY
    };
}
