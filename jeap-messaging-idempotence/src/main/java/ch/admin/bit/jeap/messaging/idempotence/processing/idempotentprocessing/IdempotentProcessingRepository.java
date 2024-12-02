package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;

import java.time.ZonedDateTime;

public interface IdempotentProcessingRepository {

    /**
     * Create a new idempotent processing record with the given identity if no such record already exists.
     *
     * @param id The idempotent processing id
     *
     * @return <code>true</code> if a new idempotent processing record has been created, <code>false</code> otherwise.
     */
    boolean createIfNotExists(IdempotentProcessingIdentity id);

    /**
     * Delete all idempotent processing records that have been created before the given point in time.
     *
     * @param createdBefore Messages created before this point in time will be deleted.
     *
     * @return The number of records deleted.
     */
    int deleteAllCreatedBefore(ZonedDateTime createdBefore);

}
