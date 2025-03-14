package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstanceState;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ContextConfiguration(classes = SequenceInstanceRepositoryTest.TestConfig.class)
class SequenceInstanceRepositoryTest {

    @EnableJpaRepositories
    @EntityScan(basePackageClasses = SequenceInstance.class)
    @ComponentScan
    static class TestConfig {
    }

    @Autowired
    private SequenceInstanceRepository sequenceInstanceRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    void save_newSequenceInstance() {
        SequenceInstance se = createSequenceInstance("name", "contextId1");
        SequenceInstance persistedSequenceInstance = sequenceInstanceRepository.save(se);
        assertThat(persistedSequenceInstance).isNotNull();

        SequenceInstance se2 = createSequenceInstance("name2", "contextId1");
        SequenceInstance persistedSequenceInstance2 = sequenceInstanceRepository.save(se2);
        assertThat(persistedSequenceInstance2).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void save_sameNameContextId_throwsException() {

        sequenceInstanceRepository.save(createSequenceInstance("name", "contextId2"));
        SequenceInstance sequenceInstance = createSequenceInstance("name", "contextId2");

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> sequenceInstanceRepository.save(sequenceInstance));

        ConstraintViolationException cause = (ConstraintViolationException) exception.getCause();
        assertThat(cause.getConstraintName())
                .contains("SEQUENCE_INSTANCE_NAME_CONTEXT_ID_UK");
    }

    @Test
    void findByTypeAndContextId() {
        String name = UUID.randomUUID().toString();
        String contextId = UUID.randomUUID().toString();
        testEntityManager.persist(createSequenceInstance(name, contextId));
        testEntityManager.persist(createSequenceInstance(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        Optional<SequenceInstance> result = sequenceInstanceRepository.findByNameAndContextId(name, contextId);

        assertThat(result).isPresent();
    }

    private SequenceInstance createSequenceInstance(String name, String contextId) {
        return SequenceInstance.builder()
                .name(name)
                .contextId(contextId)
                .state(SequenceInstanceState.OPEN)
                .retentionPeriod(Duration.ofMinutes(1))
                .build();
    }

}
