# jEAP Messaging

jEAP Messaging is a library for sending and receiving messages over Apache Kafka in Spring Boot
applications. It builds on [Spring for Apache Kafka](https://spring.io/projects/spring-kafka) and Avro,
and lets a microservice produce and consume strongly-typed events and commands with minimal
boilerplate. It provides:

* A structured definition of message types using Avro schemas and message-type descriptors
* Avro serialization of message types to Kafka records
* Pre-configured Spring Kafka beans for producers and consumers (single- and multi-cluster)
* Declaration of consumed/produced message types on topics using Java annotations (message contracts)
* Built-in error handling that never loses a message (integration with a `jeap-error-handling-service`)
* Idempotent message handling, message signing and encryption, and a Kafka broker health indicator

For the transactional outbox pattern see the separate
[jeap-messaging-outbox](https://github.com/jeap-admin-ch/jeap-messaging-outbox) library.

## Documentation

Start with [Getting started](docs/getting-started.md), then follow the links below.

| Topic                                                   | File                                                                             |
|---------------------------------------------------------|----------------------------------------------------------------------------------|
| Getting started (add the dependency, produce & consume) | [docs/getting-started.md](docs/getting-started.md)                               |
| Architecture & three-layer model                        | [docs/architecture.md](docs/architecture.md)                                     |
| Choosing dependencies                                   | [docs/dependencies.md](docs/dependencies.md)                                     |
| Configuration reference (`jeap.messaging.kafka.*`)      | [docs/configuration.md](docs/configuration.md)                                   |
| Message types (events & commands)                       | [docs/message-types.md](docs/message-types.md)                                   |
| Defining messages in Avro                               | [docs/defining-messages.md](docs/defining-messages.md)                           |
| Avro Maven plugin                                       | [docs/avro-maven-plugin.md](docs/avro-maven-plugin.md)                           |
| Schema evolution                                        | [docs/message-evolution.md](docs/message-evolution.md)                           |
| Message Type Registry                                   | [docs/message-type-registry.md](docs/message-type-registry.md)                   |
| Publishing messages                                     | [docs/publishing-messages.md](docs/publishing-messages.md)                       |
| Consuming messages                                      | [docs/consuming-messages.md](docs/consuming-messages.md)                         |
| Kafka how-to (integrate Kafka into a service)           | [docs/kafka-how-to.md](docs/kafka-how-to.md)                                     |
| Kafka topics & client configuration                     | [docs/kafka-topics-and-configuration.md](docs/kafka-topics-and-configuration.md) |
| Message contracts                                       | [docs/message-contracts.md](docs/message-contracts.md)                           |
| Idempotent message handler                              | [docs/idempotent-message-handler.md](docs/idempotent-message-handler.md)         |
| Error handling                                          | [docs/error-handling.md](docs/error-handling.md)                                 |
| Confluent Schema Registry                               | [docs/schema-registry-confluent.md](docs/schema-registry-confluent.md)           |
| AWS Glue Schema Registry                                | [docs/schema-registry-aws-glue.md](docs/schema-registry-aws-glue.md)             |
| AWS MSK IAM authentication                              | [docs/aws-msk-iam-authentication.md](docs/aws-msk-iam-authentication.md)         |
| Signing messages                                        | [docs/signing-messages.md](docs/signing-messages.md)                             |
| Encrypting messages                                     | [docs/encrypting-messages.md](docs/encrypting-messages.md)                       |
| Message filtering                                       | [docs/message-filtering.md](docs/message-filtering.md)                           |
| Health indicators                                       | [docs/health-indicators.md](docs/health-indicators.md)                           |
| Testing                                                 | [docs/testing.md](docs/testing.md)                                               |

## Modules

The artifact most consumers depend on is `jeap-messaging-infrastructure-kafka`. Group id for all
modules is `ch.admin.bit.jeap`; the version is managed by the jEAP Spring Boot parent.

| Module                                                     | Purpose                                                                                             |
|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `jeap-messaging-model`                                     | Infrastructure-independent interfaces for domain events and commands                                |
| `jeap-messaging-api`                                       | `MessageListener` / `MessagePublisher` interfaces                                                   |
| `jeap-messaging-avro`                                      | Avro implementation of the model: `AvroMessage`, builders, `AvroSerializationHelper`                |
| `jeap-messaging-avro-maven-plugin`                         | Generates Java classes from Avro schemas / the Message Type Registry                                |
| `jeap-messaging-avro-compiler`, `-validator`, `-test`      | Internal Avro compilation, validation and test support                                              |
| `jeap-messaging-avro-errorevent`                           | Avro types for the error-handling integration                                                       |
| `jeap-messaging-infrastructure`                            | Core Kafka infrastructure: serialization, signing, crypto, tracing, metrics, health, error handling |
| `jeap-messaging-infrastructure-kafka`                      | Spring Boot auto-configuration; the main consumer-facing artifact                                   |
| `jeap-messaging-infrastructure-kafka-test`                 | Test fixtures: `TestMessageSender`, `TestKafkaListener`, `KafkaIntegrationTestBase`                 |
| `jeap-messaging-infrastructure-kafka-without-tracing-test` | Test fixtures for services that do not use tracing                                                  |
| `jeap-messaging-confluent-schema-registry`                 | Confluent Schema Registry integration                                                               |
| `jeap-messaging-glue-schema-registry`                      | AWS Glue Schema Registry integration                                                                |
| `jeap-messaging-aws-msk-iam-auth`                          | AWS MSK IAM authentication                                                                          |
| `jeap-messaging-idempotence`                               | `@IdempotentMessageHandler` with a JPA-backed processing store and housekeeping                     |
| `jeap-messaging-contract-annotations`                      | Message-contract annotations                                                                        |
| `jeap-messaging-contract-annotation-processor`             | Annotation processor generating the contract JSON uploaded during the build                         |
| `jeap-messaging-contract-maven-plugin`                     | Maven plugin for message contracts                                                                  |
| `jeap-messaging-registry-maven-plugin`                     | Verifies and exports a Message Type Registry                                                        |

## Changes

This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Note

This repository is part the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
