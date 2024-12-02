package ch.admin.bit.jeap.domainevent.avro;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;

/**
 * Exceptions to be throws within an {@link AvroDomainEventBuilder}
 *
 * @deprecated Use {@link AvroMessageBuilderException} instead
 */
@Deprecated(forRemoval = true)
public class DomainEventException extends AvroMessageBuilderException {

    /**
     * Just here so that there is a constructor
     */


    private DomainEventException(String message) {
        super(message);
    }
}
