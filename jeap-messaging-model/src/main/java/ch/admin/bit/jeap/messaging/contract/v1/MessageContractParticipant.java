package ch.admin.bit.jeap.messaging.contract.v1;

import lombok.NonNull;
import lombok.Value;

@Value
public class MessageContractParticipant {
    @NonNull
    String system;
    @NonNull
    String service;
}
