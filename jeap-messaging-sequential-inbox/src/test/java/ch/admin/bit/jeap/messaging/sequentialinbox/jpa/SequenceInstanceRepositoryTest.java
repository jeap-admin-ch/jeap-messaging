package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.SequenceInstanceState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ContextConfiguration(classes = SequenceInstanceRepositoryTest.TestConfig.class)
class SequenceInstanceRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @EnableJpaRepositories
    @EntityScan(basePackageClasses = SequenceInstance.class)
    @ComponentScan
    static class TestConfig {
    }

    @Autowired
    private SequenceInstanceRepository sequenceInstanceRepository;

    @Test
    void save_newSequenceInstance() {
        SequenceInstance se = createSequenceInstance("name", "contextId1");
        long persistedSequenceInstanceId = sequenceInstanceRepository.saveNewInstance(se);

        SequenceInstance se2 = createSequenceInstance("name2", "contextId1");
        long persistedSequenceInstance2 = sequenceInstanceRepository.saveNewInstance(se2);
        assertThat(persistedSequenceInstance2)
                .isEqualTo(persistedSequenceInstanceId + 1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void save_sameNameContextId_throwsException() {

        sequenceInstanceRepository.saveNewInstance(createSequenceInstance("name", "contextId2"));
        SequenceInstance sequenceInstance = createSequenceInstance("name", "contextId2");

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> sequenceInstanceRepository.saveNewInstance(sequenceInstance));

        assertThat(exception.getCause().getMessage())
                .containsIgnoringCase("duplicate key value violates unique constraint \"sequence_instance_name_context_id_uk\"");
    }

    @Test
    void findByTypeAndContextId() {
        String name = UUID.randomUUID().toString();
        String contextId = UUID.randomUUID().toString();
        sequenceInstanceRepository.saveNewInstance(createSequenceInstance(name, contextId));
        sequenceInstanceRepository.saveNewInstance(createSequenceInstance(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        Optional<Long> result = sequenceInstanceRepository.findIdByNameAndContextId(name, contextId);

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
