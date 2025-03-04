package ch.admin.bit.jeap.messaging.sequentialinbox.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@NoArgsConstructor(access = PROTECTED) // for JPA
@Getter
@Entity
@Table(name = "sequenced_message")
public class SequencedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sm_sequence")
    @SequenceGenerator(name = "sm_sequence", sequenceName = "sequenced_message_sequence", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "message_type")
    private String messageType;

    @Column(name = "sequenced_message_id")
    private UUID sequencedMessageId;

    @Column(name = "idempotence_id")
    private String idempotenceId;

    @Column(name = "cluster_name")
    private String clusterName;

    @Column(name = "topic")
    private String topic;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    @Setter
    private SequencedMessageState state;

    @Embedded
    private SequentialInboxTraceContext traceContext;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "state_changed_at")
    private ZonedDateTime stateChangedAt;

    @Column(name = "sequence_instance_id")
    private long sequenceInstanceId;

    @Builder
    private SequencedMessage(long sequenceInstanceId, String messageType, UUID sequencedMessageId, String idempotenceId, String clusterName, String topic, SequencedMessageState state, SequentialInboxTraceContext traceContext) {
        this.messageType = messageType;
        this.sequencedMessageId = sequencedMessageId;
        this.idempotenceId = idempotenceId;
        this.clusterName = clusterName;
        this.topic = topic;
        this.state = state;
        this.traceContext = traceContext;
        this.createdAt = ZonedDateTime.now();
        this.sequenceInstanceId = sequenceInstanceId;
    }

    @Override
    public String toString() {
        return "SequencedMessage{" +
                "id=" + id +
                ", messageType='" + messageType + '\'' +
                ", sequencedMessageId=" + sequencedMessageId +
                ", idempotenceId='" + idempotenceId + '\'' +
                ", state=" + state +
                ", sequenceInstanceId=" + sequenceInstanceId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SequencedMessage that = (SequencedMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
