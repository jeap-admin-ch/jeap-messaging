# Getting started

This page shows how to add jEAP Messaging to a Spring Boot service and produce and consume a message
over Kafka. For the bigger picture see [Architecture](architecture.md); for an end-to-end checklist
when wiring Kafka into a new service see the [Kafka how-to](kafka-how-to.md).

## 1. Add the dependency

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-messaging-infrastructure-kafka</artifactId>
</dependency>
```

The version is managed by the jEAP Spring Boot parent. This single artifact pulls in the Avro model
and the Spring Boot auto-configuration. See [Choosing dependencies](dependencies.md) for the other
modules (schema registries, idempotence, test fixtures).

## 2. Add the message-type dependency

A service can only produce or consume message types for which a Java binding exists. Those bindings
are generated from the [Message Type Registry](message-type-registry.md) and consumed as normal Maven
dependencies:

```xml
<dependency>
    <groupId>ch.admin.bit.jme.messagetype.jme</groupId>
    <artifactId>jme-declaration-created-event</artifactId>
    <version>1.4.0</version>
</dependency>
```

## 3. Configure the cluster

The minimum configuration points the service at a Kafka cluster and a schema registry. All cluster
properties live under `jeap.messaging.kafka.*` (see the full [Configuration reference](configuration.md)).

```yaml
jeap:
  messaging:
    kafka:
      cluster:
        default-cluster:
          bootstrapServers: localhost:9092
          schemaRegistryUrl: http://localhost:8081
      systemName: JME
      serviceName: ${spring.application.name}
      errorTopicName: jme-messageprocessing-failed
```

`systemName` and `errorTopicName` are required for the [error handler](error-handling.md) to report
failures; `serviceName` defaults to `${spring.application.name}`.

## 4. Declare message contracts

Every produced and consumed message type needs a [message contract](message-contracts.md), declared
with an annotation anywhere in the service (typically on the `@SpringBootApplication` class):

```java
@JeapMessageProducerContract(JmeDeclarationCreatedEvent.TypeRef.class)
@JeapMessageConsumerContract(JmeDeclarationCreatedEvent.TypeRef.class)
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 5. Publish a message

Build the message with its generated builder and send it with a `KafkaTemplate`. See
[Publishing messages](publishing-messages.md) for details and the `MessagePublisher` abstraction.

```java
@Component
@RequiredArgsConstructor
class DeclarationPublisher {
    private final KafkaTemplate<AvroMessageKey, AvroMessage> kafkaTemplate;

    void publish(String declarationId) {
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId(declarationId)
                .declarationIdReference(declarationId)
                .build();
        kafkaTemplate.send("jme-messaging-declaration-created", event);
    }
}
```

## 6. Consume a message

A consumer is a Spring `@KafkaListener`. Always acknowledge **after** processing — see
[Consuming messages](consuming-messages.md).

```java
@Component
class DeclarationConsumer {
    @KafkaListener(topics = "jme-messaging-declaration-created")
    void consume(JmeDeclarationCreatedEvent event, Acknowledgment ack) {
        // ... process the event ...
        ack.acknowledge();
    }
}
```

Because delivery is at-least-once, make consumers [idempotent](idempotent-message-handler.md).

## Related

- [Architecture](architecture.md)
- [Configuration reference](configuration.md)
- [Kafka how-to](kafka-how-to.md)
- [Message contracts](message-contracts.md)
- [Testing](testing.md)
