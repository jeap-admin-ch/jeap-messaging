package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;

@RequiredArgsConstructor
public class JpaIdempotentProcessingRepository implements IdempotentProcessingRepository {

    private final SpringDataJpaIdempotentProcessingRepository springDataRepository;

    @Override
    public boolean createIfNotExists(IdempotentProcessingIdentity id) {
        return springDataRepository.createIfNotExists(id.getId(), id.getContext(), ZonedDateTime.now()) != 0;
    }

    @Override
    public int deleteAllCreatedBefore(ZonedDateTime createdBefore) {
        int deleteCount = springDataRepository.countCreatedAtBefore(createdBefore);
        springDataRepository.deleteByCreatedAtBefore(createdBefore);
        return deleteCount;
    }
}
