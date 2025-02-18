package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Entity
@Table(name = "sequence_instance", uniqueConstraints = {@UniqueConstraint(name = "SEQUENCE_INSTANCE_TYPE_CONTEXT_ID_UK", columnNames = {"type", "context_id"})})
public class SequenceInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "si_sequence")
    @SequenceGenerator(name = "si_sequence", sequenceName = "sequence_instance_sequence", allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "context_id")
    private String contextId;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private SequenceInstanceState state;

    @OneToMany(mappedBy = "sequenceInstance",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @ToString.Exclude
    private List<SequencedMessage> messages;

    public SequenceInstance(String type, String contextId, SequenceInstanceState state) {
        this.type = type;
        this.contextId = contextId;
        this.state = state;
        this.messages = new LinkedList<>();
    }
}
