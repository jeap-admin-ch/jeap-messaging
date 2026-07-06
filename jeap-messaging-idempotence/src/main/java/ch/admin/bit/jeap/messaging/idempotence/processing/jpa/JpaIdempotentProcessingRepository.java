package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;

@RequiredArgsConstructor
public class JpaIdempotentProcessingRepository implements IdempotentProcessingRepository {

    private final SpringDataJpaIdempotentProcessingRepository springDataRepository;

    @Getter
    private final boolean onConflictDoNothingInsert;

    @Override
    public boolean createIfNotExists(IdempotentProcessingIdentity id) {
        if (onConflictDoNothingInsert) {
            return springDataRepository.createIfNotExistsOnConflictDoNothing(id.getId(), id.getContext(), ZonedDateTime.now()) != 0;
        }
        return springDataRepository.createIfNotExists(id.getId(), id.getContext(), ZonedDateTime.now()) != 0;
    }

    @Override
    public int deleteAllCreatedBefore(ZonedDateTime createdBefore) {
        int deleteCount = springDataRepository.countCreatedAtBefore(createdBefore);
        springDataRepository.deleteByCreatedAtBefore(createdBefore);
        return deleteCount;
    }
}
