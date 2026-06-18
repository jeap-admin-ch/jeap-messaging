# Publishing messages

A producer builds an Avro message with its generated builder and sends it through a
`KafkaTemplate<AvroMessageKey, AvroMessage>`. For the default cluster, inject the `KafkaTemplate`
directly; for a non-default cluster add `@Qualifier("<clusterName>")` on the injected template (see
[Configuration reference](configuration.md)).

## A publisher component

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

Each message type ships with a generated builder (e.g. `JmeDeclarationCreatedEventBuilder`). These
builders extend a jEAP base class — `AvroDomainEventBuilder` for events and `AvroCommandBuilder` for
commands (from `jeap-messaging-avro`) — which auto-fills the `MessageType` information (type name,
version, identity, timestamps; see [Defining messages](defining-messages.md)). Always build messages
through these builders rather than constructing the Avro objects by hand, and set a meaningful
`idempotenceId`, as consumers rely on it to deduplicate.

`kafkaTemplate.send(topic, message)` returns a future. To send synchronously, block on it with
`.get(timeout)`.

## Choosing the topic

A message type has a default topic taken from its registry descriptor; you can also send to an
explicit topic name as shown above.

## The `MessagePublisher` abstraction

`jeap-messaging-api` defines a `MessagePublisher` interface. Implement a publisher class that decides
where messages go, and inject it into your message generators. This is a system-layer concept that
keeps the routing decision out of the business code — see [Architecture](architecture.md).

## Producer contract required

A producer contract is REQUIRED for every produced type (see
[Message contracts](message-contracts.md)). The `publishWithoutContractAllowed` property must be
`false` in production (see [Configuration reference](configuration.md)).

## Multi-cluster caveat with Lombok

When using a `@Qualifier("<clusterName>")` on a constructor-injected `KafkaTemplate`, note that
Lombok does NOT copy field annotations to generated constructors by default. Configure
`lombok.copyableAnnotations` so the qualifier reaches the generated constructor, otherwise the wrong
cluster's template may be injected. See
[https://projectlombok.org/features/constructor](https://projectlombok.org/features/constructor).

## Related

- [jeap-messaging](../README.md)
- [Consuming messages](consuming-messages.md)
- [Message contracts](message-contracts.md)
- [Defining messages](defining-messages.md)
- [Configuration reference](configuration.md)
