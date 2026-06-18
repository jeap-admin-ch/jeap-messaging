# Defining messages

A message type is defined as an Avro schema (JSON) or Avro IDL. The naming conventions are
load-bearing: the [Avro Maven plugin](avro-maven-plugin.md) and the validator add and validate code
based on them.

## DomainEvent records

For a `DomainEvent` the record name must end in `Event` and define these fields:

| Field                | Type                                                         | Notes                                                                                                                                                                                   |
|----------------------|--------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `identity`           | `ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity` | Has `eventId`, `idempotenceId` (string), `created` (long)                                                                                                                               |
| `type`               | `AvroDomainEventType`                                        | `name`, `version` strings                                                                                                                                                               |
| `publisher`          | `AvroDomainEventPublisher`                                   | `system`, `service` strings                                                                                                                                                             |
| `references`         | Any record whose name ends in `References`                   | Fields must be records whose names end in `Reference`, each with a `type` string field plus identifier fields; references and identifiers may be optional via `union {null, ...}`       |
| `payload`            | Optional; name ends in `Payload`                             | Types used INSIDE a payload must NOT end in `Event`/`Command`/`Reference`/`References`/`MessageKey` unless they really are such types, or the plugin's convention-based handling breaks |
| `domainEventVersion` | string                                                       | —                                                                                                                                                                                       |
| `processId`          | optional string                                              | —                                                                                                                                                                                       |
| `user`               | `AvroDomainEventUser`                                        | optional                                                                                                                                                                                |

## Command records

For a `Command` the record name must end in `Command` and uses
`ch.admin.bit.jeap.messaging.avro.AvroMessageIdentity` (fields `id`, `idempotenceId`, `created`),
`AvroMessageType`, `AvroMessagePublisher`, `commandVersion` and `AvroMessageUser`.

## Avro IDL example

```
import idl "DomainEventBaseTypes.avdl";

record DeclarationCreatedEvent {
  AvroDomainEventIdentity identity;
  AvroDomainEventType type;
  AvroDomainEventPublisher publisher;
  string domainEventVersion;
  union { null, DeclarationCreatedReferences } references = null;
  union { null, DeclarationCreatedPayload } payload = null;
}
record DeclarationCreatedReferences {
  DeclarationReference declaration;
}
record DeclarationReference {
  string type = "Declaration";
  string id;
}
record DeclarationCreatedPayload {
  string status;
}
```

## Builders

Extend `ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder` (events) or
`ch.admin.bit.jeap.command.avro.AvroCommandBuilder` (commands). Always use a builder so the
`MessageType` info is auto-filled: `type.name` comes from the (unqualified) schema name; `version`
from the generated class's version field (if generated from the registry) or
`getSpecifiedMessageTypeVersion()`; `variant` is settable.

## Listener and testing

Extend the `MessageListener` interface for a message-type-specific listener.

`ch.admin.bit.jeap.messaging.avro.AvroSerializationHelper` (de)serializes a message to/from Avro
WITHOUT Kafka — use it to unit-test that a builder produces a schema-valid, (de)serializable message.

## Message keys

A key is an optional Avro schema whose record name must end in `MessageKey`. Keys control
partitioning — only use them when ordering is required (see
[Kafka topics & client configuration](kafka-topics-and-configuration.md)).

## Related

- [Message types](message-types.md)
- [Avro Maven plugin](avro-maven-plugin.md)
- [Message Type Registry](message-type-registry.md)
- [Publishing messages](publishing-messages.md)
- [Testing](testing.md)
