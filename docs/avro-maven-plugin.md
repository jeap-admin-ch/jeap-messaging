# Avro Maven plugin

`jeap-messaging-avro-maven-plugin` generates Java classes from Avro definitions and binds them to the
jEAP message interfaces, so the generated event/command classes implement the model types from
`jeap-messaging-avro` / `jeap-messaging-model`.

Most services do not run this plugin directly: their message-type classes arrive as Maven dependencies
from the [Message Type Registry](message-type-registry.md), which builds them with this same plugin.
You use the plugin directly when you keep Avro definitions in your own repository — for example a
shared message library, or local/dev message types not yet in the registry.

## Goals

| Goal                            | Input                       | Default phase       | Purpose                                                         |
|---------------------------------|-----------------------------|---------------------|-----------------------------------------------------------------|
| `idl`                           | `.avdl` (Avro IDL)          | `generate-sources`  | Generate Java from Avro IDL files                               |
| `protocol`                      | `.avpr` (Avro protocol)     | `generate-sources`  | Generate Java from Avro protocol files                          |
| `schema`                        | `.avsc` (Avro schema)       | `process-resources` | Generate Java from Avro schema files                            |
| `compile-message-types`         | registry `descriptor/` tree | `generate-sources`  | Build a whole Message Type Registry into per-type Maven modules |
| `deploy-message-type-artifacts` | generated modules           | `deploy`            | Deploy the generated message-type JARs                          |

`idl`, `protocol` and `schema` are the goals an application or message library uses. The last two are
used inside a Message Type Registry build and are documented under
[Message Type Registry](message-type-registry.md).

## Generating classes from Avro IDL

Add `jeap-messaging-avro` (it provides the base types the generated classes reference) and run the
`idl` goal on the `.avdl` files in `src/main/avro`:

```xml
<dependency>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-messaging-avro</artifactId>
</dependency>
```

```xml
<plugin>
    <groupId>ch.admin.bit.jeap</groupId>
    <artifactId>jeap-messaging-avro-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>idl</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Use `schema` or `protocol` instead of (or alongside) `idl` if your definitions are `.avsc` or `.avpr`
files. In Avro IDL, import the jEAP base types with
`import idl "DomainEventBaseTypes.avdl";` (events) or `import idl "MessagingBaseTypes.avdl";`
(commands).

## Parameters

The `idl`, `protocol` and `schema` goals share these parameters:

| Parameter                  | Default                                        | Description                                                                              |
|----------------------------|------------------------------------------------|------------------------------------------------------------------------------------------|
| `sourceDirectory`          | `${basedir}/src/main/avro`                     | Directory scanned for Avro definition files                                              |
| `outputDirectory`          | `${project.build.directory}/generated-sources` | Where generated Java is written; added as a compile source root                          |
| `deleteBaseEventFiles`     | `true`                                         | Remove the generated base-type classes, since they already ship in `jeap-messaging-avro` |
| `enableDecimalLogicalType` | `false`                                        | Map Avro `decimal` logical types to `BigDecimal`                                         |

## Naming conventions

The naming conventions are load-bearing: the plugin adds and validates code based on them (payload
records end in `Payload`, references in `References`/`Reference`, keys in `MessageKey`, values in
`Event`/`Command`). A schema that violates them fails the build. See
[Defining messages](defining-messages.md).

## Related

- [Defining messages](defining-messages.md)
- [Message types](message-types.md)
- [Message Type Registry](message-type-registry.md)
