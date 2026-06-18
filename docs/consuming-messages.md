# Consuming messages

A consumer is a Spring `@KafkaListener` method taking the typed message and an `Acknowledgment`. For
the default cluster, omit `containerFactory`; for a non-default cluster set
`containerFactory = "<clusterName>KafkaListenerContainerFactory"` (see [Configuration reference](configuration.md)).

## A listener method

jeap-messaging sets `enable.auto.commit=false` and `AckMode=MANUAL`. You MUST always acknowledge with
`ack.acknowledge()` at the END of processing — once the message was either processed successfully or
handed to the error handler. NEVER acknowledge before processing completes, or messages may be lost.

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

## Make consumers idempotent

Delivery is at-least-once, so duplicates are possible. Make consumers idempotent. The automatic
approach is the [idempotent message handler](idempotent-message-handler.md). The manual alternative is
to persist and check the message's idempotence id:

```java
String idempotenceId = event.getIdentity().getIdempotenceId();
if (alreadyProcessed(idempotenceId)) {
    ack.acknowledge();
    return;
}
```

## Consumer contract required

A consumer contract is REQUIRED (see [Message contracts](message-contracts.md)). The
`consumeWithoutContractAllowed` property must be `false` in production. When listening to a topic that
carries several event types, set `silentIgnoreWithoutContract=true` to suppress the error logged for
types you don't process (see [Configuration reference](configuration.md)).

## Overriding the deserialized type

Set the listener properties `specific.avro.value.type` / `specific.avro.key.type` to deserialize into
a specific (compatible) type — used for self-message schema evolution (see
[Message evolution](message-evolution.md)):

```java
@KafkaListener(topics = TOPIC_NAME,
        properties = {"specific.avro.value.type=ch.admin.bit.jme.test.JmeSimpleTestV2Event"})
public void consume(JmeSimpleTestV2Event event, Acknowledgment ack) { ... }
```

## Accessing the message key

To access message keys in the consumer, set `expose-message-key-to-consumer=true` (see
[Configuration reference](configuration.md)).

## Related

- [jeap-messaging](../README.md)
- [Publishing messages](publishing-messages.md)
- [Idempotent message handler](idempotent-message-handler.md)
- [Error handling](error-handling.md)
- [Message contracts](message-contracts.md)
- [Configuration reference](configuration.md)
