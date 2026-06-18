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
Annotate the handler (or a surrounding method) with `@Transactional`. On a concurrent create
(`DataIntegrityViolationException` / `PessimisticLockingFailureException`) it throws
`IdempotentMessageHandlerExecutionSkippedException`.

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

## Storage

Storage is JPA-backed. The identity `IdempotentProcessingIdentity` is
`{idempotence_id, idempotence_id_context}`. A datasource and the backing table are required.

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
