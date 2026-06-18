# Choosing dependencies

All modules share the group id `ch.admin.bit.jeap`; versions are managed by the jEAP Spring Boot
parent, so declare dependencies without a `<version>`. The full module list is in
[jeap-messaging](../README.md). This page maps common needs to the artifact you depend on.

## Core

| You want to…                                                                   | Depend on                                                        |
|--------------------------------------------------------------------------------|------------------------------------------------------------------|
| Produce and consume messages over Kafka                                        | `jeap-messaging-infrastructure-kafka`                            |
| Compile against the Avro message model only (e.g. in a shared message library) | `jeap-messaging-avro`                                            |
| Use the `MessageListener` / `MessagePublisher` abstractions                    | brought in transitively by `jeap-messaging-infrastructure-kafka` |

`jeap-messaging-infrastructure-kafka` is the one artifact most services need. It transitively brings
in `jeap-messaging-infrastructure`, `jeap-messaging-avro`, `jeap-messaging-api` and
`jeap-messaging-model`.

## Schema registry & cloud

| You want to…               | Depend on / configure                                                                   |
|----------------------------|-----------------------------------------------------------------------------------------|
| Confluent Schema Registry  | included; configure `schemaRegistryUrl` — see [Confluent](schema-registry-confluent.md) |
| AWS Glue Schema Registry   | `jeap-messaging-glue-schema-registry` — see [AWS Glue](schema-registry-aws-glue.md)     |
| AWS MSK IAM authentication | `jeap-messaging-aws-msk-iam-auth` — see [MSK IAM](aws-msk-iam-authentication.md)        |

## Optional features

| You want to…                              | Depend on                                                                                         |
|-------------------------------------------|---------------------------------------------------------------------------------------------------|
| Automatic idempotent message handling     | `jeap-messaging-idempotence` — see [Idempotent message handler](idempotent-message-handler.md)    |
| Generate Java classes from Avro schemas   | `jeap-messaging-avro-maven-plugin` (build plugin) — see [Avro Maven plugin](avro-maven-plugin.md) |
| Message contract annotations on their own | `jeap-messaging-contract-annotations` (normally transitive via the generated message-type JARs)   |

## Testing

| You want to…                                                              | Depend on (test scope)                                     |
|---------------------------------------------------------------------------|------------------------------------------------------------|
| EmbeddedKafka integration tests, `TestMessageSender`, `TestKafkaListener` | `jeap-messaging-infrastructure-kafka-test`                 |
| The same without OpenTelemetry tracing                                    | `jeap-messaging-infrastructure-kafka-without-tracing-test` |

See [Testing](testing.md).

## Message types

Generated message-type bindings are **not** part of this library — they are published by your
program's [Message Type Registry](message-type-registry.md) and consumed as separate dependencies,
e.g. `ch.admin.bit.jme.messagetype.jme:jme-declaration-created-event`.

## Related

- [Getting started](getting-started.md)
- [Architecture](architecture.md)
- [Testing](testing.md)
