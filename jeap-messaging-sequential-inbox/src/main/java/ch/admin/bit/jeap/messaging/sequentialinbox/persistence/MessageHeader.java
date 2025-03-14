package ch.admin.bit.jeap.messaging.sequentialinbox.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static lombok.AccessLevel.PROTECTED;

@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "message_header")
public class MessageHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hm_sequence")
    @SequenceGenerator(name = "hm_sequence", sequenceName = "message_header_sequence", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "header_name")
    private String headerName;

    @Column(name = "header_value")
    private byte[] headerValue;

    @ManyToOne
    @JoinColumn(name="buffered_message_id", nullable=false)
    private BufferedMessage bufferedMessage;

    @Builder
    private MessageHeader(String headerName, byte[] headerValue, BufferedMessage bufferedMessage) {
        this.headerName = headerName;
        this.headerValue = headerValue;
        this.bufferedMessage = bufferedMessage;
    }
}