package ch.admin.bit.jeap.messaging.annotations;

import ch.admin.bit.jeap.messaging.avro.MessageTypeMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Annotation declares multiple message consumer contracts for an application.
 * <ul>
 * <li>
 *     value (mandatory): Message type metadata class, generated by the jEAP messaging avro maven plugin (TypeRef).
 *     Example: value = JmeCreateDeclarationCommand.TypeRef.class
 * </li>
 * <li>
 *     appName: If not set, the 'spring.application.name' property in 'application.y[a]ml' or 'bootstrap.y[a]ml' will be used.
 * </li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface JeapMessageConsumerContracts {
    Class<? extends MessageTypeMetadata>[] value();

    String appName() default "";
}