# AGENTS.md

Guidance for AI coding agents working **in this repository**. For how to *use* the library in a
consuming service, read [README.md](README.md) and the [docs/](docs/) folder instead.

## Project

jEAP Messaging is a multi-module Maven library for messaging over Apache Kafka in Spring Boot,
built on Spring for Apache Kafka and Avro. It serializes strongly-typed events and commands to Kafka
records, wires up pre-configured producer/consumer beans through Spring Boot auto-configuration, and
adds jEAP-specific concerns on top: message contracts, an at-least-once error handler, idempotent
handling, message signing and encryption, schema-registry integrations and a Kafka health indicator.

## Repository layout

```
pom.xml                                          # Parent POM (packaging=pom); declares the modules below
jeap-messaging-model/                            # Infrastructure-independent event/command model interfaces
jeap-messaging-api/                              # MessageListener / MessagePublisher interfaces
jeap-messaging-avro/                             # Avro model impl: AvroMessage, builders, AvroSerializationHelper
jeap-messaging-avro-maven-plugin/                # Generates Java classes from Avro schemas / Message Type Registry
jeap-messaging-avro-compiler/                    # Avro compilation internals used by the plugin
jeap-messaging-avro-validator/                   # Avro schema naming-convention validation
jeap-messaging-avro-test/                        # Avro test helpers
jeap-messaging-avro-errorevent/                  # Avro types for the error-handling integration
jeap-messaging-infrastructure/                   # Core Kafka infra: serdes, signing, crypto, tracing, metrics, health, error handling
jeap-messaging-infrastructure-kafka/             # @AutoConfiguration wiring; main consumer-facing artifact
jeap-messaging-infrastructure-kafka-test/        # Test fixtures (EmbeddedKafka base class, TestMessageSender, TestKafkaListener)
jeap-messaging-infrastructure-kafka-without-tracing-test/  # Test fixtures without OpenTelemetry tracing
jeap-messaging-confluent-schema-registry/        # Confluent Schema Registry integration
jeap-messaging-glue-schema-registry/             # AWS Glue Schema Registry integration
jeap-messaging-aws-msk-iam-auth/                 # AWS MSK IAM authentication
jeap-messaging-idempotence/                      # @IdempotentMessageHandler, JPA store, housekeeping
jeap-messaging-contract-annotations/             # @JeapMessage(Producer|Consumer)Contract(s) annotations
jeap-messaging-contract-annotation-processor/    # APT processor producing the contract JSON
jeap-messaging-contract-maven-plugin/            # Maven plugin for message contracts
jeap-messaging-registry-maven-plugin/            # Verifies/exports a Message Type Registry
Jenkinsfile, publiccode.yml, CHANGELOG.md, LICENSE
```

Dependency direction (consumer-facing): `model` ← `avro` ← `infrastructure` ← `infrastructure-kafka`.
The `api` module holds the `MessageListener`/`MessagePublisher` abstractions; the schema-registry and
MSK modules plug into `infrastructure-kafka` and are activated by configuration.

## Build & test

```bash
./mvnw -pl <module> -am install      # build a module and its dependencies
./mvnw verify                        # full build incl. tests
./mvnw -pl jeap-messaging-infrastructure-kafka test
```

- Parent: `ch.admin.bit.jeap:jeap-internal-spring-boot-parent` (Spring Boot 4 aligned).
- Integration tests use `@EmbeddedKafka`; extend `KafkaIntegrationTestBase`
  (`jeap-messaging-infrastructure-kafka-test`). Every feature must have Spring Boot integration tests.
- Spring Boot 3 maintenance happens on the `release/springboot3` branch; `master` targets Spring Boot 4.

## jEAP conventions

- Java packages live under `ch.admin.bit.jeap.messaging...` (and `ch.admin.bit.jeap.domainevent.avro`
  for the Avro event base types).
- Configuration properties use the prefix `jeap.messaging.kafka.*` (signing/auth uses
  `jeap.messaging.authentication.*`, idempotence uses `jeap.messaging.idempotent-*`).
- Auto-configuration is registered via `@AutoConfiguration` and
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- All Kafka keys and values must be Avro objects extending the base types from `jeap-messaging-avro`
  (`AvroMessage`, `AvroMessageKey`).
- **Avro naming conventions are load-bearing** — the Avro Maven plugin and validator add and validate
  code based on them: value record names end in `Event` or `Command`, references types end in
  `References`/`Reference`, payload types end in `Payload`, key types end in `MessageKey`. Types used
  *inside* a payload must not end in any of those suffixes unless they really are such a type.

## Docs

When changing public behaviour, update the matching focused file under [docs/](docs/) (one topic per
file) and the documentation index in the README.

## Versioning

- Semantic Versioning; all changes documented in [CHANGELOG.md](./CHANGELOG.md) (Keep a Changelog format).
- `setPomVersions.sh` updates the version across all module POMs.
- When working on a feature branch, increase the version to `x.y.z-SNAPSHOT` in the POMs.
- Always keep the -SNAPSHOT postfix in the POMs, CI will remove it when releasing a version. Do not use the SNAPSHOT
  postfix in other places (CHANGELOG, publiccode.yml etc)
- Keep changelog entries concise and to the point, follow existing patterns
- Keep commit messages short, use the JIRA ID from the branch name as a prefix, do not use conventional commits (for
  example: "JEAP-1234 Added feature X")
- When bumping the version, also update the changelog, and update version/date in `publiccode.yml`.
- When the version on a feature branch has not yet been bumped compared to master, ask the user if a major, minor or
  patch version bump should be performed, and update the version accordingly.
