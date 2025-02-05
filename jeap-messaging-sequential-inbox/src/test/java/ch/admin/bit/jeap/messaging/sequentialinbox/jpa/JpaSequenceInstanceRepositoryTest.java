package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ContextConfiguration(classes = SequentialInboxJpaConfig.class)
class JpaSequenceInstanceRepositoryTest {

    @Autowired
    private JpaSequenceInstanceRepository jpaSequenceInstanceRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    void save_newSequenceInstance() {
        SequenceInstance se = createSequenceInstance("type", "contextId");
        SequenceInstance persistedSequenceInstance = jpaSequenceInstanceRepository.save(se);
        assertThat(persistedSequenceInstance).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void save_sameTypeContextId_throwsException() {
        jpaSequenceInstanceRepository.save(createSequenceInstance("type", "contextId"));
        SequenceInstance sequenceInstance = createSequenceInstance("type", "contextId");

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> jpaSequenceInstanceRepository.save(sequenceInstance));

        assertThat(exception.getMessage())
                .startsWith("could not execute statement [Unique index or primary key violation: \"PUBLIC.SEQUENCE_INSTANCE_TYPE_CONTEXT_ID_UK");
    }

    @Test
    void findAll() {
        testEntityManager.persist(createSequenceInstance("type", "contextId-1"));
        testEntityManager.persist(createSequenceInstance("type", "contextId-2"));
        testEntityManager.persist(createSequenceInstance("type", "contextId-3"));

        List<SequenceInstance> results = jpaSequenceInstanceRepository.findAll();

        assertThat(results).hasSize(3);
    }

    @Test
    void findByTypeAndContextId() {
        String type = UUID.randomUUID().toString();
        String contextId = UUID.randomUUID().toString();
        testEntityManager.persist(createSequenceInstance(type, contextId));
        testEntityManager.persist(createSequenceInstance(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        List<SequenceInstance> results = jpaSequenceInstanceRepository.findByTypeAndContextId(type, contextId);

        assertThat(results).hasSize(1);
    }

    private SequenceInstance createSequenceInstance(String type, String contextId) {

        SequenceInstance sequenceInstance = new SequenceInstance(
                type,
                contextId,
                SequenceInstanceState.OPEN);

        BufferedMessage bufferedMessage = new BufferedMessage(
                null,
                "value".getBytes(StandardCharsets.UTF_8),
                "headers");

        sequenceInstance.getMessages().add(new SequencedMessage(
                "MyEvent1",
                UUID.randomUUID(),
                SequencedMessageState.PROCESSING,
                Duration.ofHours(1),
                SequentialInboxTraceContext.builder().traceId(1L).spanId(1L).parentSpanId(1L).build(),
                sequenceInstance,
                bufferedMessage
        ));

        sequenceInstance.getMessages().add(new SequencedMessage(
                "MyEvent2",
                UUID.randomUUID(),
                SequencedMessageState.PROCESSING,
                Duration.ofMinutes(55),
                SequentialInboxTraceContext.builder().traceId(1L).spanId(1L).parentSpanId(1L).build(),
                sequenceInstance,
                bufferedMessage
        ));

        return sequenceInstance;
    }

}