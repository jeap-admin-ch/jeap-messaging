package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessing;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for the idempotent processing repository. Implemented by the tests that run this contract against
 * the different databases / insert strategies supported by the repository, ensuring identical behavior on all of
 * them. Tests implementing this contract must run without a test-managed transaction (the repository methods
 * demarcate their own transactions, and the cleanup in {@link #deleteAllIdempotentProcessingRecords()} must commit).
 */
public interface IdempotentProcessingRepositoryContract {

    IdempotentProcessingRepository getRepository();

    SpringDataJpaIdempotentProcessingRepository getSpringDataRepository();

    @BeforeEach
    default void assertNoIdempotentProcessingRecords() {
        assertThat(getSpringDataRepository().findAll()).isEmpty();
    }

    @AfterEach
    default void deleteAllIdempotentProcessingRecords() {
        // Without @Transactional on the test, rows committed by createIfNotExists persist
        // across test methods and must be cleaned up explicitly.
        getSpringDataRepository().deleteAll();
    }

    @Test
    default void testCreateIfNotExists() {
        final IdempotentProcessingIdentity id1Ctx1 = IdempotentProcessingIdentity.from("id1", "ctx1");
        final IdempotentProcessingIdentity id1Ctx2 = IdempotentProcessingIdentity.from("id1", "ctx2");
        final IdempotentProcessingIdentity id2Ctx1 = IdempotentProcessingIdentity.from("id2", "ctx1");
        final IdempotentProcessingIdentity id2Ctx2 = IdempotentProcessingIdentity.from("id2", "ctx2");

        assertThat(getRepository().createIfNotExists(id1Ctx1)).isTrue();
        assertThat(getRepository().createIfNotExists(id1Ctx1)).isFalse();

        assertThat(getRepository().createIfNotExists(id1Ctx2)).isTrue();
        assertThat(getRepository().createIfNotExists(id1Ctx2)).isFalse();

        assertThat(getRepository().createIfNotExists(id2Ctx1)).isTrue();
        assertThat(getRepository().createIfNotExists(id2Ctx1)).isFalse();

        assertThat(getRepository().createIfNotExists(id2Ctx2)).isTrue();
        assertThat(getRepository().createIfNotExists(id2Ctx2)).isFalse();

        assertThat(getRepository().createIfNotExists(id1Ctx1)).isFalse();
        assertThat(getRepository().createIfNotExists(id1Ctx2)).isFalse();
        assertThat(getRepository().createIfNotExists(id2Ctx1)).isFalse();
        assertThat(getRepository().createIfNotExists(id2Ctx2)).isFalse();
    }

    @Test
    default void testDeleteAllCreatedBefore() throws InterruptedException {
        getRepository().createIfNotExists(IdempotentProcessingIdentity.from("id1", "ctx"));
        getRepository().createIfNotExists(IdempotentProcessingIdentity.from("id2", "ctx"));
        getRepository().createIfNotExists(IdempotentProcessingIdentity.from("id3", "ctx"));
        Thread.sleep(100);
        final ZonedDateTime afterCreatingFirstThreeIds = ZonedDateTime.now();
        getRepository().createIfNotExists(IdempotentProcessingIdentity.from("id4", "ctx"));
        getRepository().createIfNotExists(IdempotentProcessingIdentity.from("id5", "ctx"));
        Thread.sleep(100);
        final ZonedDateTime afterCreatingLastTwoIds = ZonedDateTime.now();
        assertIdempotenceIdsExistOnly("id1", "id2", "id3", "id4", "id5");

        int numDeletedFirstDelete = getRepository().deleteAllCreatedBefore(afterCreatingFirstThreeIds);
        assertThat(numDeletedFirstDelete).isEqualTo(3);
        assertIdempotenceIdsExistOnly("id4", "id5");

        int numDeletedSecondDelete = getRepository().deleteAllCreatedBefore(afterCreatingLastTwoIds);
        assertThat(numDeletedSecondDelete).isEqualTo(2);
        assertThat(getSpringDataRepository().findAll()).isEmpty();
    }

    private void assertIdempotenceIdsExistOnly(String... idempotenceIds) {
        assertThat(
                getSpringDataRepository().findAll().stream()
                        .map(IdempotentProcessing::getId)
                        .map(IdempotentProcessingIdentity::getId)
                        .collect(Collectors.toSet())
        ).containsOnly(idempotenceIds);
    }

}
