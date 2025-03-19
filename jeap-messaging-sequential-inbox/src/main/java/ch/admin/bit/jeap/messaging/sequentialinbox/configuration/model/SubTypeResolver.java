package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;

/**
 * Resolves a subtype for a given Avro message. Used for generic events/messages that represent different
 * business events using the same avro message type. Messages can then be sequenced based on the type + subtype.
 */
public interface SubTypeResolver<M extends AvroMessage, E extends Enum<E>> {

    /**
     * @param avroMessage The avro message type to determine the subtype of to use when sequencing messages
     * @return the subtype of the given avro message type, never null
     */
    E resolveSubType(M avroMessage);

}
