package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings("JpaDataSourceORMInspection")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "deferred_message")
public class DeferredMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dm_sequence")
    @SequenceGenerator(name = "dm_sequence", sequenceName = "deferred_message_sequence", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @Column(name = "message")
    private byte[] message;

    @ToString.Exclude
    @Column(name = "`key`")
    private byte[] key;

    @Column(name = "clusterName")
    private String clusterName;

    @Column(name = "topic")
    private String topic;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "message_idempotence_id")
    private String messageIdempotenceId;

    @Column(name = "message_type_name")
    private String messageTypeName;

    @Column(name = "message_type_version")
    private String messageTypeVersion;

    @Column(name = "created")
    private ZonedDateTime created;

    @Column(name = "send_immediately")
    private boolean sendImmediately;

    @Column(name = "schedule_after")
    private ZonedDateTime scheduleAfter;

    @Column(name = "sent_immediately")
    private ZonedDateTime sentImmediately;

    @Column(name = "sent_scheduled")
    private ZonedDateTime sentScheduled;

    @Column(name = "failed")
    private ZonedDateTime failed;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason")
    private SendFailureReason failReason;

    @Column(name = "resend")
    private boolean resend;

    @Embedded
    private OutboxTraceContext traceContext;

    @Builder
    public static DeferredMessage createDeferredMessage(@NonNull byte[] message, byte[] key, String clusterName, @NonNull String topic,
                                                        @NonNull String messageId, @NonNull String messageIdempotenceId,
                                                        @NonNull String messageTypeName, String messageTypeVersion,
                                                        boolean sendImmediately, OutboxTraceContext traceContext) {
        return new DeferredMessage(null, message, key, clusterName, topic, messageId, messageIdempotenceId, messageTypeName, messageTypeVersion,
                ZonedDateTime.now(), sendImmediately, null, null, null, null, null, false, traceContext);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeferredMessage)) {
            return false;
        }
        DeferredMessage other = (DeferredMessage) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
