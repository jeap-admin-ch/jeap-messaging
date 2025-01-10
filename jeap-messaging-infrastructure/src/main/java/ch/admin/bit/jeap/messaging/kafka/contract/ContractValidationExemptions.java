package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.model.MessageType;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Set;

@UtilityClass
public class ContractValidationExemptions {

    private static final Collection<String> SHARED_MESSAGES_ALLOWED_TO_SEND =
            Set.of("MessageProcessingFailedEvent", "ProcessTaskPlannedEvent", "ProcessTaskCompletedEvent", "ProcessInstanceCompletedEvent", "CreateProcessInstanceCommand");
    private static final Collection<String> SHARED_MESSAGES_ALLOWED_TO_RECEIVE =
            Set.of("ProcessInstanceCreatedEvent", "ProcessMilestoneReachedEvent", "ProcessInstanceCompletedEvent", "CreateProcessInstanceCommand");

    public static boolean isExemptedFromSenderValidation(MessageType messageType) {
        return SHARED_MESSAGES_ALLOWED_TO_SEND.contains(messageType.getName());
    }

    public static boolean isExemptedFromReceiverValidation(String messageTypeName) {
        return SHARED_MESSAGES_ALLOWED_TO_RECEIVE.contains(messageTypeName);
    }

}
