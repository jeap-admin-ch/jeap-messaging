# Message Type Registry

All message types used within a microservice environment must be documented in a Message Type Registry
— one per program/application group (e.g. DaziT). It gives an overview of subscriber/publisher
dependencies. It is a Git repository with one type descriptor per type; a validation build keeps it
consistent and prevents changing existing schema versions.

> **Note:** The registry is a **separate** repository, not part of jeap-messaging. jeap-messaging
> provides the tooling: `jeap-messaging-registry-maven-plugin` (verify/export) and
> `jeap-messaging-avro-maven-plugin` (generate Java bindings).

## Structure

Descriptors live under `/descriptor/<system-lowercase>/event/<eventname>/` (and `/command/`), with a
`<Type>.json` descriptor plus a `<type>_<version>.avdl` value schema and an optional key schema next to
it. `/schema` holds JSON schemas for descriptors (usable in the IDE). Shared records go under
`/descriptor/<system>/_common` or `/descriptor/_common`; a record checked into master must never be
changed or deleted (create a new record with a new name, e.g. a version suffix).

## Descriptor data model

Top-level descriptor fields:

| Field              | Content                                                         | Mandatory |
|--------------------|-----------------------------------------------------------------|-----------|
| `name`             | Message type name (legacy alias: `eventName` / `commandName`)   | Y         |
| `definingSystem`   | System that defines the type (legacy alias: `publishingSystem`) | Y         |
| `description`      | Human-readable description of the type                          | Y         |
| `scope`            | `public` (crosses system boundaries) or `internal`              | Y         |
| `documentationUrl` | Link to further documentation                                   | N         |
| `topic`            | Default topic for the type                                      | N         |
| `topics`           | List of topics for shared types (alternative to `topic`)        | N         |
| `versions`         | List of version entries (see below)                             | Y         |

> **Note:** `name` replaces the legacy `eventName` / `commandName`, and `definingSystem` replaces the
> legacy `publishingSystem`. The old names are still accepted as deprecated aliases.

Each entry in `versions` describes one schema version:

| Field               | Content                                                                                                      | Mandatory |
|---------------------|--------------------------------------------------------------------------------------------------------------|-----------|
| `version`           | Semantic version, e.g. `1.2.0`                                                                               | Y         |
| `valueSchema`       | `.avdl` value schema file next to the descriptor                                                             | Y         |
| `keySchema`         | `.avdl` key schema file                                                                                      | N         |
| `compatibilityMode` | `BACKWARD` / `FORWARD` / `FULL` / `NONE` — compatibility to the previous version (or to `compatibleVersion`) | N         |
| `compatibleVersion` | The version this schema must be compatible with (default: the previous semver version)                       | N         |

The example below declares a type with three versions; each new version states the compatibility it
keeps to its predecessor (see [Message evolution](message-evolution.md)):

```json
{
  "name": "JmeDeclarationCreatedEvent",
  "definingSystem": "JME",
  "description": "Published when a customs declaration is created",
  "scope": "public",
  "topic": "jme-messaging-declaration-created",
  "versions": [
    {
      "version": "1.0.0",
      "valueSchema": "JmeDeclarationCreatedEvent_v1.avdl",
      "keySchema": "ch.admin.bit.jme.declaration.BeanReferenceMessageKey.avdl"
    },
    {
      "version": "1.1.0",
      "valueSchema": "JmeDeclarationCreatedEvent_v2.avdl",
      "keySchema": "ch.admin.bit.jme.declaration.BeanReferenceMessageKey.avdl",
      "compatibilityMode": "BACKWARD"
    },
    {
      "version": "2.0.0",
      "valueSchema": "JmeDeclarationCreatedEvent_v3.avdl",
      "keySchema": "ch.admin.bit.jme.declaration.BeanReferenceMessageKey.avdl",
      "compatibilityMode": "NONE"
    }
  ]
}
```

## Maintenance

Check out, create a branch, commit, open a PR to master; the verification pipeline (also runnable
locally with `mvn verify`) must pass before merge. Each new version needs a migration strategy (see
[Message evolution](message-evolution.md)).

## Java bindings

The registry build uploads generated JARs to a Maven repository; consume them with the group id
defined in the registry pom and an artifact id composed of application + message name (see
[Getting started](getting-started.md), [Defining messages](defining-messages.md)).

## Relationship to the Kafka Schema Registry

The Message Type Registry is the **design-time** home of schemas and descriptors; the Kafka Schema
Registry is the **runtime** home; jeap-messaging registers runtime schemas automatically.

## Related

- [Message evolution](message-evolution.md)
- [Defining messages](defining-messages.md)
- [Message contracts](message-contracts.md)
- [Architecture](architecture.md)
