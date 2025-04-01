package ch.admin.bit.jeap.messaging.sequentialinbox.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

import static lombok.AccessLevel.PROTECTED;

@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "sequence_instance")
public class SequenceInstance {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "context_id")
    private String contextId;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private SequenceInstanceState state;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "closed_at")
    private ZonedDateTime closedAt;

    @Column(name = "retain_until", nullable = false)
    private ZonedDateTime retainUntil;

    @Builder
    private SequenceInstance(@NonNull String name, @NonNull String contextId, SequenceInstanceState state, @NonNull Duration retentionPeriod) {
        this.name = name;
        this.contextId = contextId;
        this.state = state == null ? SequenceInstanceState.OPEN : state;
        this.createdAt = ZonedDateTime.now();
        this.retainUntil = this.createdAt.plus(retentionPeriod);
    }

    public void close() {
        if (state == SequenceInstanceState.OPEN) {
            state = SequenceInstanceState.CLOSED;
            closedAt = ZonedDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SequenceInstance that = (SequenceInstance) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
