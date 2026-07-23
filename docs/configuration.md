# Configuration reference

All properties use the prefix `jeap.messaging.kafka`. Cluster-specific options live under
`jeap.messaging.kafka.cluster.<name>.*`; for backwards compatibility most of them can also be set
directly under `jeap.messaging.kafka.*` (this does not apply to the AWS-specific properties).

For Kafka client tuning (consumer/producer/topic) via `spring.kafka.*`, see
[Kafka topics & client configuration](kafka-topics-and-configuration.md).

## Clusters

Since version 6, jEAP Messaging can connect to more than one Kafka cluster (mainly for hybrid-cloud
use cases). Each cluster is configured under a freely chosen name. Avoid special characters, as the
name is used for Spring bean names.

```yaml
jeap:
  messaging:
    kafka:
      cluster:
        bit:
          bootstrapServers: ...
          default-cluster: true   # optional; the first cluster is the default if none is marked
        aws:
          bootstrapServers: ...
```

Producers select a non-default cluster with a `@Qualifier("<name>")` on the injected `KafkaTemplate`;
consumers select it with `containerFactory = "<name>KafkaListenerContainerFactory"` on `@KafkaListener`. The
default cluster needs neither.

| Name                                               | Default          | Description                                                                                                                                                                     |
|----------------------------------------------------|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cluster.<name>.bootstrapServers`                  | `localhost:9092` | Kafka bootstrap servers (host:port)                                                                                                                                             |
| `cluster.<name>.consumerBootstrapServers`          | —                | Override bootstrap servers for consumers (since 4.2.0)                                                                                                                          |
| `cluster.<name>.producerBootstrapServers`          | —                | Override bootstrap servers for producers (since 4.2.0)                                                                                                                          |
| `cluster.<name>.adminClientBootstrapServers`       | —                | Override bootstrap servers for admin clients (since 4.2.0)                                                                                                                      |
| `cluster.<name>.default-producer-cluster-override` | `false`          | Make this cluster the default producer cluster for migration scenarios (mirroring). Affects which `KafkaTemplate` and `DefaultKafkaProducerFactory` beans are marked `@Primary` |

## Core

| Name                             | Default                      | Description                                                                                                 |
|----------------------------------|------------------------------|-------------------------------------------------------------------------------------------------------------|
| `useSchemaRegistry`              | `true`                       | Use a schema registry. If `false`, an internal mock Confluent schema registry is used                       |
| `autoRegisterSchema`             | `true`                       | Automatically register new schemas before first send (uses `TopicRecordNameStrategy`)                       |
| `expose-message-key-to-consumer` | `false`                      | Make message keys available to the consumer (`true`) or suppress them (`false`)                             |
| `systemName`                     | —                            | Name of the sending system; required to generate `MessageProcessingFailedEvent` messages                    |
| `serviceName`                    | `${spring.application.name}` | Name of the sending service; used in generated `MessageProcessingFailedEvent` messages                      |
| `errorTopicName`                 | —                            | Error topic for `MessageProcessingFailedEvent` messages (also per cluster: `cluster.<name>.errorTopicName`) |
| `errorServiceRetryIntervalMs`    | `5000`                       | Retry interval for sending a `MessageProcessingFailedEvent`                                                 |
| `errorServiceRetryAttempts`      | `5`                          | Number of attempts to send a `MessageProcessingFailedEvent` before the application terminates               |
| `embedded`                       | —                            | Force (`true`) or disable (`false`) the automatic EmbeddedKafka test configuration                          |

## Contracts

| Name                            | Default | Description                                                                                                                           |
|---------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------|
| `publishWithoutContractAllowed` | `false` | Allow producing without a contract. **Must be `false` in production**                                                                 |
| `consumeWithoutContractAllowed` | `false` | Allow consuming without a contract. **Must be `false` in production**                                                                 |
| `silentIgnoreWithoutContract`   | `false` | Suppress only the log statement emitted when a message without a contract is received (e.g. when listening to a topic carrying several event types). Contract enforcement still applies |

See [Message contracts](message-contracts.md).

## Encryption

| Name                            | Default | Description                                                                                                                                                                                          |
|---------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `messageTypeEncryptionDisabled` | `false` | Disable encryption for message types that declare an `encryptionKeyId` in their producer contract — useful in tests so no jEAP-Crypto instance is needed. **Forbidden on acceptance and production** |

See [Encrypting messages](encrypting-messages.md).

## Schema registry & Kafka authentication

For the Confluent Schema Registry with SASL authentication, the AWS Glue Schema Registry and AWS MSK
IAM authentication properties, see the dedicated pages:

- [Confluent Schema Registry](schema-registry-confluent.md)
- [AWS Glue Schema Registry](schema-registry-aws-glue.md)
- [AWS MSK IAM authentication](aws-msk-iam-authentication.md)

## Overriding the deserialized type

Since 7.2.0 (Confluent) the deserialized key/value type can be set per listener with the
`specific.avro.key.type` / `specific.avro.value.type` properties. This is needed for self-messages
during [schema evolution](message-evolution.md):

```java

@KafkaListener(topics = TOPIC_NAME,
        properties = {"specific.avro.value.type=ch.admin.bit.jme.test.JmeSimpleTestV2Event"})
public void consume(JmeSimpleTestV2Event event, Acknowledgment ack) { ...}
```

## Related

- [Getting started](getting-started.md)
- [Kafka topics & client configuration](kafka-topics-and-configuration.md)
- [Health indicators](health-indicators.md)
- [Message contracts](message-contracts.md)
