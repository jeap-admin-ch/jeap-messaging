package ch.admin.bit.jeap.messaging.annotations;

import ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata;

import java.lang.annotation.*;

/**
 * This Annotation declares a message consumer contract for an application.
 * <ul>
 * <li>value (mandatory): Message type metadata class, generated by the jEAP messaging avro maven plugin (TypeRef).
 *     Example: value = JmeCreateDeclarationCommand.TypeRef.class
 * </li>
 * <li>appName: If not set, the 'spring.application.name' property in 'application.y[a]ml' or 'bootstrap.y[a]ml' will be used.
 * Otherwise <code>NoAppNameFoundInAnnotationException</code> will be thrown
 * </li>
 * <li>
 * topics: Name of the topic(s) this messages is consumed from as String[]. If not set, DEFAULT_TOPIC from <code>value</code> will be used
 * </li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(JeapMessageConsumerContractContainer.class)
public @interface JeapMessageConsumerContract {
    Class<? extends MessageTypeMetadata> value();

    String appName() default "";

    String[] topic() default {};
}