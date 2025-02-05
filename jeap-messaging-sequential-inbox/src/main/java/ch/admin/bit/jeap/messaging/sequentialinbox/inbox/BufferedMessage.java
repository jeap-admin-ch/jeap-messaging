package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "buffered_message")
public class BufferedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bm_sequence")
    @SequenceGenerator(name = "bm_sequence", sequenceName = "buffered_message_sequence", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ToString.Exclude
    @Column(name = "message_key")
    private byte[] key;

    @ToString.Exclude
    @Column(name = "message_value")
    private byte[] value;

    @Column(name = "headers")
    private String headers;

    public BufferedMessage(byte[] key, byte[] value, String headers) {
        this.key = key;
        this.value = value;
        this.headers = headers;
    }
}
