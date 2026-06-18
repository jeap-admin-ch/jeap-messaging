# Message types

Every message exchanged through jEAP Messaging must conform to a well-defined type. Two kinds exist:
**Events** and **Commands**. Both carry message info (identifier, type), the publisher, an optional
list of references, and an optional payload. A custom type prescribes which references and which
payload are required. All type definitions must be collected in the
[Message Type Registry](message-type-registry.md).

## DomainEvent base structure

The "Builder default" column refers to the `AvroDomainEventBuilder` / `AvroCommandBuilder` base classes,
which fill these fields automatically.

| Field                                                                                                                    | Description                                                                              | Mandatory | Builder default          |
|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|-----------|--------------------------|
| `domainEventVersion`                                                                                                     | Semver of the event structure                                                            | Y         | `1.2.0`                  |
| `processId`                                                                                                              | Id of a business process                                                                 | N         | —                        |
| `publisher.system`                                                                                                       | Publishing system                                                                        | Y         | —                        |
| `publisher.service`                                                                                                      | Publishing service                                                                       | Y         | —                        |
| `type.name`                                                                                                              | Type name                                                                                | Y         | unqualified message name |
| `type.version`                                                                                                           | Semver of the type                                                                       | Y         | —                        |
| `type.variant`                                                                                                           | Distinguishes events of the same type business-wise                                      | N         | —                        |
| `identity.eventId` (alias `id`)                                                                                          | Unique event id                                                                          | Y         | random UUID              |
| `identity.idempotenceId`                                                                                                 | Must carry the SAME value if the same event is re-published under at-least-once delivery | Y         | —                        |
| `identity.created`                                                                                                       | Unix timestamp (ms, UTC)                                                                 | Y         | current timestamp        |
| `user.id`, `familyName`, `givenName`, `businessPartnerName`, `businessPartnerId`, `propertiesMap` (`Map<String,String>`) | User info                                                                                | N         | —                        |
| `references`                                                                                                             | List of references                                                                       | N         | —                        |
| `payload`                                                                                                                | Message payload                                                                          | N         | —                        |

## Command base structure

A `Command` has the same shape as a `DomainEvent`, except it uses `commandVersion` instead of
`domainEventVersion` and its identity field is named `id` (there is no `eventId` alias). The "Builder
default" column refers to the `AvroCommandBuilder` base class.

| Field                                                                                                                    | Description                                                                                | Mandatory | Builder default          |
|--------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|-----------|--------------------------|
| `commandVersion`                                                                                                         | Semver of the command structure                                                            | Y         | `1.2.0`                  |
| `processId`                                                                                                              | Id of a business process                                                                   | N         | —                        |
| `publisher.system`                                                                                                       | Publishing system                                                                          | Y         | —                        |
| `publisher.service`                                                                                                      | Publishing service                                                                         | Y         | —                        |
| `type.name`                                                                                                              | Type name                                                                                  | Y         | unqualified message name |
| `type.version`                                                                                                           | Semver of the type                                                                         | Y         | —                        |
| `type.variant`                                                                                                           | Distinguishes commands of the same type business-wise                                      | N         | —                        |
| `identity.id`                                                                                                            | Unique message id                                                                          | Y         | random UUID              |
| `identity.idempotenceId`                                                                                                 | Must carry the SAME value if the same command is re-published under at-least-once delivery | Y         | —                        |
| `identity.created`                                                                                                       | Unix timestamp (ms, UTC)                                                                   | Y         | current timestamp        |
| `user.id`, `familyName`, `givenName`, `businessPartnerName`, `businessPartnerId`, `propertiesMap` (`Map<String,String>`) | User info                                                                                  | N         | —                        |
| `references`                                                                                                             | List of references                                                                         | N         | —                        |
| `payload`                                                                                                                | Message payload                                                                            | N         | —                        |

## References

Each reference must define a `type` (the business-object type it points to). Distinguish three things:

| Role                                                         | Type of reference                                                    | Type of business object                                    |
|--------------------------------------------------------------|----------------------------------------------------------------------|------------------------------------------------------------|
| Field name in the `...References` record (e.g. `old`, `new`) | The field's record type, ending in `Reference` (e.g. `AhvReference`) | The `type` string field on the reference (e.g. `"Person"`) |

```
record DeclarationCreatedReferences {
  DeclarationReference declaration;
}
record DeclarationReference {
  string type = "Declaration";
  string id;
  int version;
}
```

## Payload

The payload depends on the message pattern:

| Pattern                      | Payload                                            |
|------------------------------|----------------------------------------------------|
| Event Notification           | No payload, only references                        |
| Event-Carried State Transfer | Payload required                                   |
| Event-Sourcing               | Payload required                                   |
| Commands                     | Payload often needed, sometimes references suffice |

## Guidelines

- Events must be `DomainEvent` objects and commands must be `Command` objects.
- By default prefer messages **without** payload — this reduces coupling, because only the receiver
  should know which data it needs.
- Prefer two distinct message types over using a `variant`. Variants exist mainly to distinguish
  generic events within a reaction chain.

## Related

- [Defining messages](defining-messages.md)
- [Message Type Registry](message-type-registry.md)
- [Message evolution](message-evolution.md)
- [Idempotent message handler](idempotent-message-handler.md)
