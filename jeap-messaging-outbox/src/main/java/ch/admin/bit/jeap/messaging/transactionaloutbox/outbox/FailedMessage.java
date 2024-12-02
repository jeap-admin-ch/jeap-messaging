package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class FailedMessage {

    public static FailedMessage from(DeferredMessage dm) {
        return new FailedMessage(dm.getId(), dm.getTopic(), dm.getMessageId(), dm.getMessageIdempotenceId(), dm.getMessageTypeName(),
                                 dm.getCreated(), dm.getFailed(), dm.getFailReason(), dm.isResend());
    }

    private Long id;

    private String topic;

    private String messageId;

    private String messageIdempotenceId;

    private String messageTypeName;

    private ZonedDateTime createdAt;

    private ZonedDateTime failedAt;

    private SendFailureReason failReason;

    private boolean resend;

}
