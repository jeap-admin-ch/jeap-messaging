package ch.admin.bit.jeap.messaging.idempotence.processing.jpa;

import java.util.Arrays;
import java.util.Locale;

/**
 * Strategy used to insert idempotent processing records.
 */
public enum IdempotentProcessingInsertMode {

    /**
     * Choose the insert strategy based on the database in use: {@link #ON_CONFLICT_DO_NOTHING} on PostgreSQL,
     * {@link #WHERE_NOT_EXISTS} otherwise.
     */
    AUTO,

    /**
     * Portable insert using {@code INSERT ... SELECT ... WHERE NOT EXISTS}. Works on any SQL database. A concurrent
     * insert of the same idempotent processing record by another transaction surfaces as a unique constraint
     * violation on this insert.
     */
    WHERE_NOT_EXISTS,

    /**
     * PostgreSQL-specific insert using {@code INSERT ... ON CONFLICT DO NOTHING}. A concurrent insert of the same
     * idempotent processing record by another transaction does not raise an error: this insert waits for the other
     * transaction to complete and then simply does not insert if the other transaction committed the record.
     */
    ON_CONFLICT_DO_NOTHING;

    static IdempotentProcessingInsertMode fromPropertyValue(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid idempotent processing insert mode '%s'. Valid values are %s."
                    .formatted(value, Arrays.toString(values())), e);
        }
    }

}
