# Message contracts

A message contract states what a microservice consumes or produces: the app name (the Spring
`spring.application.name`), the message-type name and version, and the topic. The schema itself
follows from the [Message Type Registry](message-type-registry.md), so a contract only references the
type — it never duplicates the schema.

Contracts serve three goals:

- document which message types are used by which producers and consumers;
- document the dependencies between producers and consumers;
- validate schema compatibility against a target environment before deployment (the "can-I-deploy"
  check).

## How contracts work

Contracts are declared with annotations in the microservice and uploaded to a Message Contract
Service during the build: an annotation processor generates a JSON contract file, which the jEAP build
pipeline uploads. At runtime a Kafka client interceptor validates that a contract exists for each
produced and consumed type; without a matching contract, producing or consuming is denied.

## Declaring contracts

Four annotations come from `jeap-messaging-contract-annotations`, normally brought in transitively via
the generated message-type JARs: `@JeapMessageConsumerContract`, `@JeapMessageProducerContract`,
`@JeapMessageConsumerContracts` and `@JeapMessageProducerContracts`. They can be placed anywhere,
commonly on the `@SpringBootApplication` class. Each references the generated `TypeRef` inner class of
the message type, e.g. `JmeCreateDeclarationCommand.TypeRef.class`.

`appName` is optional and defaults from `spring.application.name`. `topic` is optional and defaults to
the message type's default topic; an array of multiple topics is allowed, for example for shared
events.

```java
@JeapMessageProducerContract(JmeDeclarationCreatedEvent.TypeRef.class)
@JeapMessageConsumerContract(JmeCreateDeclarationCommand.TypeRef.class)
@JeapMessageConsumerContract(
        value = JmeSharedEvent.TypeRef.class,
        topic = {"my-topic", "my-topic-2"})
@JeapMessageProducerContract(
        value = JmeAuditEvent.TypeRef.class,
        appName = "my-audit-service")
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Annotation processor

The `jeap-messaging-contract-annotation-processor` runs at build time and produces the contract JSON,
which the jEAP build pipeline uploads to the Message Contract Service. Normally just having the
dependency on the classpath is enough.

Only in the special case where `annotationProcessorPaths` is configured explicitly in the
`maven-compiler-plugin` must you add the processor path explicitly:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>ch.admin.bit.jeap</groupId>
                <artifactId>jeap-messaging-contract-annotations</artifactId>
                <version>${jeap-messaging.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Template-based contracts

A template annotation `@JeapMessageConsumerContractsByTemplates` exists for the Process Context Service
and Process Archive Service to auto-generate consumer contracts from template JSON files. These are
separate services and are out of scope here.

## Configuration

For relaxing contract enforcement during development and testing, see `publishWithoutContractAllowed`,
`consumeWithoutContractAllowed` and `silentIgnoreWithoutContract` in the
[Configuration reference](configuration.md).

## Related

- [jeap-messaging](../README.md)
- [Message Type Registry](message-type-registry.md)
- [Message evolution](message-evolution.md)
- [Publishing messages](publishing-messages.md)
- [Consuming messages](consuming-messages.md)
- [Configuration reference](configuration.md)
