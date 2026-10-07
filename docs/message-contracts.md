# Message contracts

A message contract states what a microservice consumes or produces: the app name (the Spring
`spring.application.name`), the message-type name and version, and the topic. The message type is
defined in the [Message Type Registry](message-type-registry.md); the contract references the type and
is accompanied by the Avro schema of the type, which is uploaded together with the contract.

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

Framework-owned infrastructure messages, such as error-handling events produced internally by jEAP
components, can be exempt from producer validation. This does not relax contract validation for
application-owned business messages.

`ModulithPublicationProcessingFailedEvent`, published by the Modulith error handling starter through
the transactional outbox, is exempt from producer validation. Source applications still declare
consumer contracts for `RetryModulithPublicationCommand` and `DiscardModulithPublicationCommand` on
their configured topics; the enabled starter checks them at startup. This producer exemption does
not disable command-consumer validation.

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

Next to the contract files, the processor writes the Avro schema of the message type as
`schema-<identity-hash>.avsc`, read from the `SCHEMA$` field of the generated message type class.
Each contract's `schemaResource` points to its schema file. The identity includes the binary class
name, system, message type, version and registry coordinates, preventing collisions between types
with the same simple name and version. Contracts sharing that identity share one resource within a
compilation; repeated contracts reuse successful or unsuccessful extraction results for that identity.
The `jeap-messaging-contract-maven-plugin` uploads the schema together with the contract, so that the
Message Contract Service does not need to read the schema from the message type registry.

Both direct and template-based contracts read compiled message types from the compiler's classpath
or module path, including isolated `annotationProcessorPaths`. Classpath extraction is tested with
javac and Eclipse ECJ; named-module extraction is tested with javac. The processor reads the generated
`new Schema.Parser().parse(...)` literal assignment from classfile instructions without executing it.
Types compiled in the same run require javac's Trees API, which resolves the parser method before
reading its literal arguments. JSON syntax and record shape are checked without initializing Avro,
message classes, conversions or application logging. Full Avro semantic validation remains in MCS.
Unknown initializer patterns, uncertain source origins and unreadable schemas are omitted, never
replaced with stale bytecode or guessed values. Binary extraction accepts the generated straight-line
assignment at the start of the static initializer, including split string literals; custom factories
and preceding initialization code use the registry fallback.

Generation deliberately remains in the contract processor: existing message artifacts work without
being rebuilt to include schema resources. Consumers must parse schemas rather than compare JSON text.

The uploaded schema is the schema of the message type artifact the application is actually built
against, and the Message Contract Service uses it as the schema of that contract. It validates that the
schema is the Avro record schema of the declared message type and version; the registry coordinates
(`registryUrl`, `registryBranch`, `registryCommit`) stay in the contract for traceability and for the
fallback below. Message types referenced by a hand-written `MessageTypeMetadata` implementation have no
`SCHEMA$` and therefore no schema file. If a schema exists but cannot be read, the processor emits a
NOTE with the reason. In both cases the Message
Contract Service reads the schema from the message type registry as before.

Only files explicitly named by `schemaResource` are uploaded. A null or absent reference means no
schema is available, including contracts produced by earlier releases. Unreferenced files are ignored.
A missing referenced file emits a Maven warning before publishing without that schema. The publish
log reports contract counts with and without schemas. Schema files are read only once per publication;
the service API continues to receive a `schema` string on each contract.

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

Schema upload is enabled by default. To make the Message Contract Service obtain all schemas from
the registry, pass `-DmessageContractSchemaUploadEnabled=false` to the
`publish-messaging-contracts` goal, or configure the contract Maven plugin:

```xml
<configuration>
    <messageContractSchemaUploadEnabled>false</messageContractSchemaUploadEnabled>
</configuration>
```

This switch ignores all schema files, including stale files from previous builds. The annotation
processor still generates schemas during compilation.

To disable generation as well, pass `-AmessageContractSchemaGenerationEnabled=false` to the Java
compiler. Both contract processors recognize this option. They continue to generate contract JSON,
leave schema references null, and perform no schema extraction.
For the Maven compiler plugin:

```xml
<configuration>
    <compilerArgs>
        <arg>-AmessageContractSchemaGenerationEnabled=false</arg>
    </compilerArgs>
</configuration>
```

Recompile annotated sources after changing this option. The upload switch above independently ignores
schema files even when using existing build output. Neither switch changes contracts already stored in MCS.

For relaxing contract enforcement during development and testing, see `publishWithoutContractAllowed` and
`consumeWithoutContractAllowed` in the [Configuration reference](configuration.md). To suppress only the log
statement emitted when a message without a contract is received (without relaxing enforcement), see
`silentIgnoreWithoutContract`.

## Related

- [jeap-messaging](../README.md)
- [Message Type Registry](message-type-registry.md)
- [Message evolution](message-evolution.md)
- [Publishing messages](publishing-messages.md)
- [Consuming messages](consuming-messages.md)
- [Configuration reference](configuration.md)
