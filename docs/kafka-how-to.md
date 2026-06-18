# Kafka how-to

An end-to-end checklist for wiring Kafka into a service with jEAP Messaging. For the basic setup see
[Getting started](getting-started.md).

## Checklist

1. Define the message types per your business requirements.
2. Order the Kafka topics for the cluster (usually one topic per event; multiple events per topic is
   possible) following the naming conventions (see
   [Kafka topics & client configuration](kafka-topics-and-configuration.md)).
3. Define the type descriptor and Avro schema and publish them in the
   [Message Type Registry](message-type-registry.md) on a feature branch.
4. Set up an [Error Handling Service](error-handling.md) (don't forget the role ordering for
   authorization).
5. Add `jeap-messaging-infrastructure-kafka` and the generated message-type dependencies.
6. Import the Kafka cluster certificates and the schema-registry root certificate into the truststore.
7. Declare the consumer/producer [message contracts](message-contracts.md) via annotations.
8. Implement the producer and consumer; apply the idempotent-receiver pattern on the consumer.
9. Define the Kafka configuration in the service.
10. Develop and test locally and on dev; follow the idempotence guidelines.
11. Once the schema is stable, merge the registry branch to master via a pull request.

## Local development with Kafka

A trimmed `docker-compose.yml` with a KRaft-mode broker, a schema registry and a Postgres for the
error-handling service:

```yaml
services:
  broker:
    image: confluentinc/cp-kafka:latest
    hostname: broker
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@broker:9093
      KAFKA_LISTENERS: SASL_PLAINTEXT://broker:9092,CONTROLLER://broker:9093
      KAFKA_ADVERTISED_LISTENERS: SASL_PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: SASL_PLAINTEXT:SASL_PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_SASL_ENABLED_MECHANISMS: SCRAM-SHA-512
      KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: SCRAM-SHA-512
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  schema-registry:
    image: confluentinc/cp-schema-registry:latest
    depends_on:
      - broker
    ports:
      - "8081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: SASL_PLAINTEXT://broker:9092
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081

  errorhandling-db:
    image: postgres:latest
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: errorhandling
      POSTGRES_USER: errorhandling
      POSTGRES_PASSWORD: errorhandling
```

## Acknowledge and commit

jeap-messaging sets `enable.auto.commit=false` and `AckMode=MANUAL`; always `ack.acknowledge()` at the
end. The jEAP error handler acknowledges on failure after sending to the error topic. See
[Consuming messages](consuming-messages.md).

## Idempotent consumer

At-least-once delivery means a consumer may receive a record more than once (also via an error-handler
resubmit). Use the `idempotenceId` in the message identity to deduplicate; see the
[idempotent message handler](idempotent-message-handler.md).

## The two registries

The Message Type Registry is the design-time home of the schema; the Kafka Schema Registry is the
runtime home, and jeap-messaging registers schemas there automatically and transparently. See
[Message Type Registry](message-type-registry.md) and [Message evolution](message-evolution.md).

## Related

- [jeap-messaging](../README.md)
- [Getting started](getting-started.md)
- [Consuming messages](consuming-messages.md)
- [Kafka topics & client configuration](kafka-topics-and-configuration.md)
- [Message Type Registry](message-type-registry.md)
- [Error handling](error-handling.md)
