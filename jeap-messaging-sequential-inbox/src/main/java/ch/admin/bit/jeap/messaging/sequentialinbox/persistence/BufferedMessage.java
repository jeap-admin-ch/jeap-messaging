package ch.admin.bit.jeap.messaging.sequentialinbox.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

import static lombok.AccessLevel.PROTECTED;

@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "buffered_message")
public class BufferedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bm_sequence")
    @SequenceGenerator(name = "bm_sequence", sequenceName = "buffered_message_sequence", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @Column(name = "message_key")
    private byte[] key;

    @ToString.Exclude
    @Column(name = "message_value")
    private byte[] value;

    @Setter
    @Column(name = "sequenced_message_id")
    private Long sequencedMessageId;

    @Column(name = "sequence_instance_id")
    private long sequenceInstanceId;

    @Builder
    private BufferedMessage(long sequenceInstanceId, byte[] key, byte[] value) {
        this.sequenceInstanceId = sequenceInstanceId;
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BufferedMessage that = (BufferedMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
