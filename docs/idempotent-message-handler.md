# Idempotent message handler

Because Kafka delivery is at-least-once, a consumer may receive the same message more than once. The
`jeap-messaging-idempotence` module provides automatic idempotent handling so that an
already-processed message is skipped. See [Choosing dependencies](dependencies.md) for the artifact.

## `@IdempotentMessageHandler`

`@IdempotentMessageHandler` is a method-level, runtime-retained annotation. Put it on a handler method
whose **first** argument is the `Message`. An AOP aspect (`IdempotentMessageHandlerAspect`) wraps the
method:

- it derives the key from `message.getIdentity().getIdempotenceId()`;
- it derives a context from the message type name (`message.getType().getName()`) with a trailing
  version stripped (pattern `(.*)(V[0-9]+)(Event|Command)`, so e.g. `FooV2Event` → context `FooEvent`);
- it creates an `IdempotentProcessing` record via `createIfNotExists`. If the record is newly created,
  the method proceeds; if the record already exists, the method is **skipped** and returns `null`.

## Requires a transaction

The aspect requires an **active transaction** — without one it throws `IllegalStateException`.
Annotate the handler (or a surrounding method) with `@Transactional`.

```java
@Component
class DeclarationConsumer {

    @Transactional
    @IdempotentMessageHandler
    @KafkaListener(topics = "jme-messaging-declaration-created")
    void consume(JmeDeclarationCreatedEvent event, Acknowledgment ack) {
        // ... process the event ...
        ack.acknowledge();
    }
}
```

## Exception Handling

If the idempotent processing record cannot be created, the aspect throws an
`IdempotentMessageHandlerExecutionSkippedException` and the handler method is not executed. This
exception provides `MessageHandlerExceptionInformation` with temporality `TEMPORARY` and a specific
error code (`IDEMPOTENT_PROCESSING_CONCURRENT_HANDLING` or `IDEMPOTENT_PROCESSING_FAILED`), so the
jEAP [error handling](error-handling.md) resends the message automatically (instead of creating a
manual task).

## Concurrent Execution Handling

Two handler executions for the same message may start at (almost) the same time.
The idempotent processing record only becomes visible to other transactions once the
transaction that created it commits, so both executions may attempt to create the record. The unique
constraint on the record guarantees that only one of them — the *winner* — can succeed; the other
one — the *loser* — blocks on its insert until the winner's transaction completes:

- If the winner **commits**, the message counts as processed. With the `on-conflict-do-nothing`
  [insert strategy](#insert-strategy), the loser then skips the message silently. With the
  `where-not-exists` insert strategy, the loser's insert fails with a unique constraint violation and
  the aspect throws an `IdempotentMessageHandlerExecutionSkippedException` with error code
  `IDEMPOTENT_PROCESSING_CONCURRENT_HANDLING` (see [Exception Handling](#exception-handling)); the
  automatic resend by the error handling is then recognized as already processed and skipped.
- If the winner **rolls back**, its idempotent processing record is rolled back with it, the loser's
  insert succeeds, and the loser processes the message.

In both cases the loser never executes the handler method while the winner is still in flight, and
the handler's database changes are committed at most once. Note that a duplicate message consumed on
the same partition is not handled concurrently but sequentially, and is simply skipped as already
processed. Occasional concurrent executions may be normal; a persistently high rate usually points
to duplicate message publication or chronic consumer-group rebalancing and is worth investigating.

## Storage

Storage is JPA-backed. The identity `IdempotentProcessingIdentity` is
`{idempotence_id, idempotence_id_context}`. A datasource and the backing table are required.
Do not impose arbitrary length limits on these values. For PostgreSQL, use `TEXT` for both columns:

```sql
CREATE TABLE idempotent_processing
(
    idempotence_id         TEXT                     NOT NULL,
    idempotence_id_context TEXT                     NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_idempotent_processing
        PRIMARY KEY (idempotence_id, idempotence_id_context)
);
```

Existing PostgreSQL tables that used the former example's 200-character limits can be migrated with:

```sql
ALTER TABLE idempotent_processing
    ALTER COLUMN idempotence_id TYPE TEXT,
    ALTER COLUMN idempotence_id_context TYPE TEXT;
```

The same recommendation applies to other string columns unless the domain itself defines a maximum
length. The included Flyway example therefore also uses `TEXT` for the ShedLock string columns.

## Insert strategy

The `IdempotentProcessing` record is created with one of two insert strategies, selected with the
`jeap.messaging.idempotent-processing.insert-mode` property:

| Value                    | Behavior                                                                                                                                                                                                            |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `auto` (default)         | Selects `on-conflict-do-nothing` if the database is PostgreSQL (detected from the JDBC metadata at startup), `where-not-exists` otherwise.                                                                          |
| `where-not-exists`       | Portable `INSERT ... SELECT ... WHERE NOT EXISTS` insert that works on any SQL database. A record concurrently inserted by another transaction handling the same message surfaces as a unique constraint violation. |
| `on-conflict-do-nothing` | PostgreSQL-specific `INSERT ... ON CONFLICT DO NOTHING` insert. A concurrent insert by another transaction handling the same message does not raise an error: the message is skipped silently once the other transaction has committed the record. |

## Housekeeping

Old records are deleted on a schedule, coordinated via ShedLock.

| Property                                                                     | Default           | Description                                                            |
|------------------------------------------------------------------------------|-------------------|------------------------------------------------------------------------|
| `jeap.messaging.idempotent-processing.houseKeepingSchedule`                  | `0 0 4 * * *`     | Cron expression for the housekeeping job                               |
| `jeap.messaging.idempotent-processing.idempotentProcessingRetentionDuration` | `30d`             | Retention duration (30 days) before old records are deleted            |
| `jeap.messaging.idempotent-message-handler.adviceOrder`                      | lowest precedence | Order of the AOP advice — lowest precedence runs nearest to the method |

## Relationship to other mechanisms

This relies on the `idempotenceId` in the message identity (see [Message types](message-types.md)).
For a manual alternative, persist and check the `idempotenceId` yourself (see
[Consuming messages](consuming-messages.md)).

## Related

- [jeap-messaging](../README.md)
- [Consuming messages](consuming-messages.md)
- [Message types](message-types.md)
- [Error handling](error-handling.md)
- [Choosing dependencies](dependencies.md)
