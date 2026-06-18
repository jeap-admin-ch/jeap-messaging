# Testing

jEAP Messaging provides EmbeddedKafka-based test fixtures in `jeap-messaging-infrastructure-kafka-test`
(test scope). For services without tracing, use `jeap-messaging-infrastructure-kafka-without-tracing-test`
(see [Choosing dependencies](dependencies.md)).

## `KafkaIntegrationTestBase`

An abstract base annotated `@EmbeddedKafka(controlledShutdown = true, partitions = 1)`. It autowires a
`KafkaTemplate<AvroMessageKey, AvroMessage>` and a `KafkaAdmin`, and waits for all listener containers
to be assigned before each test (so no records are lost). It exposes the helpers:

- `sendSync(topic, message)`
- `sendSync(topic, key, message)`
- `sendSyncEnsuringProducerContract(topic, message)`
- `sendSyncWithHeaders(topic, message, headers...)`

## `TestMessageSender`

Static helpers used by the base class:

- `sendSync(kafkaTemplate, topic, [key,] message)` sends WITHOUT a producer-contract check
- `sendSyncEnsuringProducerContract(...)` sends WITH the contract interceptor check
- `sendSyncWithHeaders(...)` adds custom record headers

## `TestKafkaListener`

A `@KafkaListener` variant for tests with the consumer-contract check DISABLED by default (useful when
the test simulates a receiver that has no contract). The default `groupId` is
`${spring.application.name:testapp}-test`.

## `AvroSerializationHelper`

From `jeap-messaging-avro`: (de)serialize a built message to/from Avro WITHOUT Kafka. Use it to verify
a builder produces schema-valid, (de)serializable messages.

## Configuration in tests

| Property                                             | Description                                                                                        |
|------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `jeap.messaging.kafka.embedded`                      | Force (`true`) or disable (`false`) the automatic EmbeddedKafka configuration                      |
| `jeap.messaging.kafka.messageTypeEncryptionDisabled` | Set to `true` to avoid needing a crypto instance (see [Configuration reference](configuration.md)) |

Contract checks can be relaxed with `consumeWithoutContractAllowed`/`publishWithoutContractAllowed` in
non-production.

## Example

```java
// A real consumer needs a consumer contract for every type it consumes, in tests too.
@JeapMessageConsumerContract(JmeDeclarationCreatedEvent.TypeRef.class)
class DeclarationConsumerIT extends KafkaIntegrationTestBase {

    private final List<JmeDeclarationCreatedEvent> received = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "jme-messaging-declaration-created")
    void onEvent(JmeDeclarationCreatedEvent event, Acknowledgment ack) {
        received.add(event);
        ack.acknowledge();
    }

    @Test
    void consumesPublishedEvent() {
        JmeDeclarationCreatedEvent event = JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("id-1")
                .declarationIdReference("id-1")
                .build();

        sendSync("jme-messaging-declaration-created", event);

        await().untilAsserted(() -> assertThat(received).hasSize(1));
    }
}
```

The `@JeapMessageConsumerContract` is required because the listener consumes `JmeDeclarationCreatedEvent`
— just like a production consumer (see [Message contracts](message-contracts.md)). If instead you use
`@TestKafkaListener`, the consumer-contract check is disabled and no contract is needed — convenient for
listeners that exist only in the test.

## Related

- [jeap-messaging](../README.md)
- [Getting started](getting-started.md)
- [Consuming messages](consuming-messages.md)
- [Message contracts](message-contracts.md)
- [Configuration reference](configuration.md)
- [Choosing dependencies](dependencies.md)
