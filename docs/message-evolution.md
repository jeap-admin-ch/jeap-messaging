# Message evolution

Avro lets the writer and reader of a message use different schema versions. The deserializer knows the
WriterSchema and the ReaderSchema; versions are compatible as long as a valid reader message can be
instantiated from the binary message. In continuous deployment every change must be compatible —
deploying an incompatible change breaks the system.

## Forward vs backward

- **Backward compatible:** readers using the NEW schema can read messages produced with the OLD schema
  → **readers migrate first**.
- **Forward compatible:** messages written with the NEW schema can be read by consumers using the OLD
  schema → **writer migrates first**.

| Change                 | Backward | Forward |
|------------------------|----------|---------|
| Add optional field     | ✓        | ✓       |
| Add mandatory field    | ✗        | ✓       |
| Delete optional field  | ✓        | ✓       |
| Delete mandatory field | ✓        | ✗       |
| Mandatory → optional   | ✓        | ✗       |
| Optional → mandatory   | ✗        | ✓       |
| Rename with alias      | ✓        | ✓       |

Forward and backward must NOT be combined in one step — that is always incompatible.

## Handling incompatible changes

Two EMC (Expand–Migrate–Contract) strategies exist:

| Strategy                | Approach                                                                                                                                                                           | Use when                                  |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| EMC Message Evolution   | Break the change into a forward- then backward-compatible step via an intermediate schema; all participants evolve together. Preferred — no breaking change, no duplicate messages | You control/coordinate all participants   |
| EMC Message Replacement | Create a new message type with a new name (e.g. V2 in the name) / new major version, publish both in parallel, remove the old one once everyone migrated                           | You have little control over participants |

## compatibilityMode

`compatibilityMode` (`BACKWARD` / `FORWARD` / `FULL` / `NONE`) is declared per version in the
[Message Type Registry](message-type-registry.md) and verified automatically against the previous
version (or `compatibleVersion`). On a registry message-type version it should almost never be `NONE`
(that implies a breaking change).

## Kafka Schema Registry compatibility

jEAP verifies compatibility at deploy time (can-i-deploy) BEFORE schemas are registered, so since
7.3.0 the library automatically sets the subject's compatibility mode to `NONE` in the Kafka Schema
Registry. Developers usually never interact with it directly.

## Self-messages

A service consuming a type it also produces needs extra steps to evolve it under any compatibility mode
other than `FULL`, because producer and consumer of the new version are deployed in the same rolling
deployment, with no forward/backward ordering between them. See
[Evolving a self-consumed message type](how-tos/evolve-self-consumed-message-type.md) for the steps and
a worked example.

## Related

- [Evolving a self-consumed message type](how-tos/evolve-self-consumed-message-type.md)
- [Message Type Registry](message-type-registry.md)
- [Defining messages](defining-messages.md)
- [Configuration reference](configuration.md)
- [Message types](message-types.md)
