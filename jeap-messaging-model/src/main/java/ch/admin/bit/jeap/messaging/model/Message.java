package ch.admin.bit.jeap.messaging.model;

import java.util.Optional;

public interface Message {
    MessageIdentity getIdentity();

    MessagePublisher getPublisher();

    MessageType getType();

    MessageReferences getReferences();

    Optional<? extends MessageReferences> getOptionalReferences(); //NOSONAR Wildcard is required for compatibility with avro-generated code

    Optional<String> getOptionalProcessId();

    MessagePayload getPayload();

    Optional<? extends MessagePayload> getOptionalPayload(); //NOSONAR Wildcard is required for compatibility with avro-generated code

    default Optional<? extends MessageUser> getOptionalUser() {  //NOSONAR Wildcard is required for compatibility with avro-generated code
        return Optional.empty();
    }

}
