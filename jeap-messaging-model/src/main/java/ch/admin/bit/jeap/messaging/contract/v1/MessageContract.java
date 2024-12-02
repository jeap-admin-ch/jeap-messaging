package ch.admin.bit.jeap.messaging.contract.v1;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MessageContract {
    //Can be null due to backwards compatibility
    String repository;

    //Can be null due to backwards compatibility
    String branch;

    @NonNull
    @JsonAlias("publishingSystem")
    String definingSystem;

    @NonNull
    String type;

    // Can be null as defined by the message type registry schema
    String compatibilityMode;

    @NonNull
    @Singular
    List<MessageContractParticipant> publishers;

    @NonNull
    @Singular
    List<MessageContractParticipant> subscribers;
}
