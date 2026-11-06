# Using the idempotent message handler

Steps to make a Kafka listener method idempotent so an already-processed message is skipped instead of
processed twice. See [Idempotent message handler](../idempotent-message-handler.md) for how it works
under the hood (concurrency semantics, insert strategy, housekeeping).

1. Add the `jeap-messaging-idempotence` dependency — see [Choosing dependencies](../dependencies.md).
2. Provide a datasource and the backing table. For PostgreSQL:

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

   Don't impose a shorter length on either column than `TEXT` allows.

   The housekeeping job that deletes expired entries runs under [ShedLock](https://github.com/lukas-krecan/ShedLock)
   (`IdempotentProcessingSchedulingConfig` registers a `JdbcTemplateLockProvider`), so the service also
   needs the standard `shedlock` table — without it the lock is never acquired, the job never runs, and
   `idempotent_processing` grows unbounded:

   ```sql
   CREATE TABLE shedlock
   (
       name       VARCHAR(64)  NOT NULL,
       lock_until TIMESTAMP    NOT NULL,
       locked_at  TIMESTAMP    NOT NULL,
       locked_by  VARCHAR(255) NOT NULL,
       PRIMARY KEY (name)
   );
   ```

   A service that already uses ShedLock for other scheduled jobs can share its existing `shedlock`
   table — it isn't specific to idempotent message handling.
3. Annotate the handler method with `@IdempotentMessageHandler`, and make sure it (or an enclosing
   method) also carries `@Transactional` — the aspect requires an active transaction and throws
   `IllegalStateException` without one:

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

   The annotated method's **first** argument must be the `Message`. On a duplicate, the method is
   skipped and returns `null` instead of running again.

That's it for the common case — once both tables from step 2 exist, the defaults (`insert-mode: auto`, a
30-day retention housekeeping job) need no further configuration. For tuning those, or understanding
what happens when two deliveries of the same message race each other, see
[Idempotent message handler](../idempotent-message-handler.md#concurrent-execution-handling).

## Related

- [jeap-messaging](../../README.md)
- [Idempotent message handler](../idempotent-message-handler.md)
- [Consuming messages](../consuming-messages.md)
- [Choosing dependencies](../dependencies.md)
