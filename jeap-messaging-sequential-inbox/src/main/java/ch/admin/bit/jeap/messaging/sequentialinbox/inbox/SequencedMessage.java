package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "sequenced_message")
public class SequencedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sm_sequence")
    @SequenceGenerator(name = "sm_sequence", sequenceName = "sequenced_message_sequence", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "sequenced_message_id")
    private UUID sequencedMessageId;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private SequencedMessageState state;

    @Column(name = "max_delay_period")
    private Duration maxDelayPeriod;

    @Embedded
    private SequentialInboxTraceContext traceContext;

    @Column(name = "created")
    private ZonedDateTime created;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private SequenceInstance sequenceInstance;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BufferedMessage message;

    public SequencedMessage(String type, UUID sequencedMessageId, SequencedMessageState state, Duration maxDelayPeriod, SequentialInboxTraceContext traceContext, SequenceInstance sequenceInstance, BufferedMessage message) {
        this.type = type;
        this.sequencedMessageId = sequencedMessageId;
        this.state = state;
        this.maxDelayPeriod = maxDelayPeriod;
        this.traceContext = traceContext;
        this.created = ZonedDateTime.now();
        this.sequenceInstance = sequenceInstance;
        this.message = message;
    }
}
