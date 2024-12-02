package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessing;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;


import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Transactional
@DataJpaTest
@ContextConfiguration(classes = IdempotentProcessingJpaConfig.class)
public class JpaIdempotentProcessingRepositoryTest {

    @Autowired
    private IdempotentProcessingRepository repository;

    @Autowired
    private SpringDataJpaIdempotentProcessingRepository springDataRepository;

    @Test
    void testCreateIfNotExists() {
        final IdempotentProcessingIdentity id1Ctx1 = IdempotentProcessingIdentity.from("id1", "ctx1");
        final IdempotentProcessingIdentity id1Ctx2 = IdempotentProcessingIdentity.from("id1", "ctx2");
        final IdempotentProcessingIdentity id2Ctx1 = IdempotentProcessingIdentity.from("id2", "ctx1");
        final IdempotentProcessingIdentity id2Ctx2 = IdempotentProcessingIdentity.from("id2", "ctx2");

        assertThat(repository.createIfNotExists(id1Ctx1)).isTrue();
        assertThat(repository.createIfNotExists(id1Ctx1)).isFalse();

        assertThat(repository.createIfNotExists(id1Ctx2)).isTrue();
        assertThat(repository.createIfNotExists(id1Ctx2)).isFalse();

        assertThat(repository.createIfNotExists(id2Ctx1)).isTrue();
        assertThat(repository.createIfNotExists(id2Ctx1)).isFalse();

        assertThat(repository.createIfNotExists(id2Ctx2)).isTrue();
        assertThat(repository.createIfNotExists(id2Ctx2)).isFalse();

        assertThat(repository.createIfNotExists(id1Ctx1)).isFalse();
        assertThat(repository.createIfNotExists(id1Ctx2)).isFalse();
        assertThat(repository.createIfNotExists(id2Ctx1)).isFalse();
        assertThat(repository.createIfNotExists(id2Ctx2)).isFalse();
    }

    @SneakyThrows
    @Test
    void testDeleteAllCreatedBefore() {
        repository.createIfNotExists(IdempotentProcessingIdentity.from("id1", "ctx"));
        repository.createIfNotExists(IdempotentProcessingIdentity.from("id2", "ctx"));
        repository.createIfNotExists(IdempotentProcessingIdentity.from("id3", "ctx"));
        Thread.sleep(100);
        final ZonedDateTime afterCreatingFirstThreeIds = ZonedDateTime.now();
        repository.createIfNotExists(IdempotentProcessingIdentity.from("id4", "ctx"));
        repository.createIfNotExists(IdempotentProcessingIdentity.from("id5", "ctx"));
        Thread.sleep(100);
        final ZonedDateTime afterCreatingLastTwoIds = ZonedDateTime.now();
        assertIdempotenceIdsExistOnly("id1", "id2", "id3", "id4", "id5");

        int numDeletedFirstDelete = repository.deleteAllCreatedBefore(afterCreatingFirstThreeIds);
        assertThat(numDeletedFirstDelete).isEqualTo(3);
        assertIdempotenceIdsExistOnly("id4", "id5");

        int numDeletedSecondDelete = repository.deleteAllCreatedBefore(afterCreatingLastTwoIds);
        assertThat(numDeletedSecondDelete).isEqualTo(2);
        assertThat(springDataRepository.findAll()).isEmpty();
    }

    private void assertIdempotenceIdsExistOnly(String... idempotenceIds) {
        assertThat(
                springDataRepository.findAll().stream()
                        .map(IdempotentProcessing::getId)
                        .map(IdempotentProcessingIdentity::getId)
                        .collect(Collectors.toSet())
        ).containsOnly(idempotenceIds);
    }

}
