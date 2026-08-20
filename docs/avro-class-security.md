# Avro class whitelist

Since 18.0.0 (Avro 1.12.2), Avro only resolves classes from a schema when they are **trusted**. jEAP
Messaging installs a whitelist covering the jEAP and application message classes, so in the default
setup there is nothing to configure.

## Why it exists

Avro 1.12.2 hardens `org.apache.avro.util.ClassSecurityValidator`. Every class Avro resolves from a
schema — this includes *all* generated record, enum and fixed types, not only the types referenced via
the `java-class` property — has to be trusted, otherwise Avro rejects it:

```
java.lang.SecurityException: Forbidden ch.admin.bit.jeap.messaging.avro.AvroMessageUser!
This class is not trusted to be included in Avro schemas.
```

Out of the box Avro only trusts a handful of `java.lang` / `java.math` types plus whatever the system
properties `org.apache.avro.SERIALIZABLE_CLASSES` and `org.apache.avro.SERIALIZABLE_PACKAGES` allow.
Without a whitelist, no jEAP message could be serialized or deserialized.

## What is trusted by default

`ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity` installs a validator trusting

- everything Avro trusts by default (`ClassSecurityValidator.DEFAULT`, which also honours the two
  system properties above),
- the JDK types a schema can reference through `java-class` / `java-key-class`: the common
  collections (`ArrayList`, `LinkedList`, `ArrayDeque`, `HashSet`, `LinkedHashSet`, `TreeSet`,
  `HashMap`, `LinkedHashMap`, `TreeMap`, `ConcurrentHashMap` and the matching interfaces) and the
  common value types (`UUID`, the `java.time` types such as `Instant`, `LocalDate`, `LocalDateTime`,
  `OffsetDateTime`, `ZonedDateTime`, `Duration`, `Period`, and the legacy `java.util.Date` /
  `java.sql.Date` / `java.sql.Time` / `java.sql.Timestamp`), and
- the **Avro generated types** in `ch.admin.bit.jeap` and its subpackages — the jEAP message base types
  (`AvroMessageUser`, `AvroMessageType`, the error event types). Trusted **unconditionally**, configuring
  the whitelist never takes them away, and
- the **Avro generated types** in `ch.admin` and its subpackages, as long as neither property below is
  configured.

"Avro generated type" means a record or error record (`SpecificRecord`), an enum (`GenericEnumSymbol`) or
a fixed type (`SpecificFixed`) — exactly the three schema kinds Avro resolves from a schema.

That covers the jEAP message base types as well as the message types generated from the
[Message Type Registry](message-type-registry.md) into `ch.admin.*` packages.

Being a generated type is a **narrowing** condition, never a reason to trust a class on its own: it is
combined with the packages above, so a schema cannot name an entity, a Spring bean or any other class
that happens to sit under `ch.admin` — and a hand-written class cannot get itself trusted by
implementing `SpecificRecord`, because it still has to live in a trusted package.

The `ch.admin` default is deliberately wide: it is a namespace, not a list of generated types, so a
schema could in principle name any `ch.admin.*` class on the classpath. Schemas come from the schema
registry rather than from message payloads, so the practical risk is low — but a service that wants a
tighter whitelist narrows it by listing its own message-type packages under `trusted-packages`.

## Configuration

Services whose generated message types live outside `ch.admin` configure the whitelist themselves:

```yaml
jeap:
  messaging:
    avro:
      trusted-packages:
        - com.example.messaging
        - com.example.messagetype
      trusted-classes:
        - com.example.legacy.SomeSpecificType
```

| Property                               | Default | Description                                                                    |
|----------------------------------------|---------|--------------------------------------------------------------------------------|
| `jeap.messaging.avro.trusted-packages` | -       | Trusted packages, subpackages included                                          |
| `jeap.messaging.avro.trusted-classes`  | -       | Fully qualified names of individual trusted classes                             |
| `jeap.messaging.avro.security-auto-configuration.enabled` | `true` | Whether jEAP Messaging installs the whitelist on startup |

> As soon as one of the two properties is set, the wide `ch.admin` default is **no longer applied** —
> that is how a service narrows the whitelist. A service that configures packages of its own **and**
> uses message types under `ch.admin` therefore has to list those as well. List the concrete packages
> its message types are generated into, for example
> `trusted-packages: [ch.admin.bit.myservice.messaging, com.example.messaging]` — listing the bare
> `ch.admin` namespace would trust *every* class under it, including non-Avro ones, and is therefore
> wider than the built-in default it replaces.
>
> A configured package or class is trusted whatever the class is — Avro generated or not. That is what
> makes the properties usable for the non-Avro types a schema references through `java-class` /
> `java-key-class`.
>
> Avro's own defaults, the JDK collection and value types and the Avro generated types in
> `ch.admin.bit.jeap` stay trusted in any case, so configuring the whitelist can never break jEAP's own
> message types.

Wildcards are rejected — trusting all packages would defeat the purpose of the Avro hardening.

When Avro rejects a class, the `SecurityException` names both properties and lists what is currently
trusted:

```
Forbidden com.example.legacy.SomeType! This class is not trusted to be referenced from an Avro schema.
Add its package to 'jeap.messaging.avro.trusted-packages' or the class itself to
'jeap.messaging.avro.trusted-classes'. Currently trusted are the packages [] and the classes [] (any class),
the Avro generated types in [ch.admin., ch.admin.bit.jeap.], the JDK types of
AvroClassSecurity.TRUSTED_JDK_CLASSES and Avro's own defaults.
```

The first two lists are what the two properties add — empty here because nothing is configured — and the
third names the built-in packages, where only Avro generated types are trusted.

## What is trusted, in one table

`✓` trusted, `✗` rejected with a `SecurityException`. "configured" means the class or its package is
named in `trusted-packages` / `trusted-classes`.

| Class | nothing configured | `trusted-packages` set | `trusted-classes` set |
|---|---|---|---|
| Avro generated type in `ch.admin.bit.jeap.**` (jEAP message types) | ✓ | ✓ | ✓ |
| Avro generated type in `ch.admin.**` (application message types) | ✓ | ✗ ¹ | ✗ ¹ |
| Avro generated type in any other package | ✗ | ✗ | ✗ |
| JDK type on the curated list (`ArrayList`, `UUID`, `Instant`, …) | ✓ | ✓ | ✓ |
| Type Avro itself trusts (`String`, `BigDecimal`, …) | ✓ | ✓ | ✓ |
| Non-Avro class in `ch.admin.**` | ✗ | ✗ | ✗ |
| Any class in a configured package | ✗ | ✓ | ✗ |
| Any class named in `trusted-classes` | ✗ | ✗ | ✓ |
| Any other class (`java.io.File`, `java.util.Properties`, …) | ✗ | ✗ | ✗ |

¹ configuring either property replaces the wide `ch.admin` default — that is how a service narrows the
whitelist. A service that configures packages of its own and still uses `ch.admin.*` message types lists
`ch.admin` (or the concrete packages) as well; the jEAP message types stay trusted either way.

Two things fall out of the table:

- **Being an Avro type never trusts a class on its own.** It only *narrows* the two built-in packages, so
  a class under `ch.admin` is trusted when it is also a generated record, enum or fixed type — and a
  hand-written class cannot get itself trusted by implementing `SpecificRecord`, because it still has to
  live in a trusted package.
- **A configured entry is trusted whatever it is.** `trusted-packages` / `trusted-classes` do not care
  whether the class is Avro generated — that is what makes them usable for the non-Avro types a schema
  references through `java-class` / `java-key-class`.

The table is executable: `AvroClassSecurityMatrixTest` checks every row without a Spring context and
`AvroClassSecurityAutoConfigurationTest` checks them again through the `jeap.messaging.avro.*`
properties.

## When the whitelist is installed

The whitelist is global, static state in Avro and has to be in place **before the first Avro
(de)serialization**. It is installed **once per JVM**.

**In an application**, `AvroClassSecurityAutoConfiguration` installs it from a
`BeanFactoryPostProcessor`, before any other bean is created. Nothing to do.

An application that wants to install the whitelist itself switches the auto-configuration off:

```yaml
jeap:
  messaging:
    avro:
      security-auto-configuration:
        enabled: false
```

It then has to call `AvroClassSecurity.install(..)` itself, **before the first Avro
(de)serialization** — with the auto-configuration off and nothing installed, Avro rejects every
generated message class.

**Without a Spring context** - unit tests of message builders, batch jobs, `main` methods - install it
yourself before touching Avro:

```java
AvroClassSecurity.installDefaultIfMissing();                          // the default whitelist
AvroClassSecurity.install(List.of("com.example.messaging"), List.of()); // or an explicit one
```

In a JUnit test that means a `@BeforeAll`:

```java
@BeforeAll
static void installAvroClassWhitelist() {
    AvroClassSecurity.installDefaultIfMissing();
}
```

A test that keeps a message in a `static final` field has to **build it in `@BeforeAll` as well** - a
static field is initialized with the class, before any JUnit callback runs, and the whitelist would not
be in place yet:

```java
private static SomeEvent event;

@BeforeAll
static void installAvroClassWhitelistAndCreateEvent() {
    AvroClassSecurity.installDefaultIfMissing();
    event = SomeEventBuilder.create()./* ... */.build();
}
```

Only *resolving* a class trips the whitelist, which is what building or (de)serializing a message does.
Reading a generated type's `SCHEMA$` merely parses the schema and resolves nothing, so a
`static final String SCHEMA = SomeEvent.SCHEMA$.toString();` needs no special treatment.

### Why installing once

Avro validates a class the first time it resolves it from a schema and caches the result in
`SpecificData`; later lookups return the cached class without validating it again. A whitelist
installed after the first (de)serialization would therefore not apply to the classes already in use -
narrowing it would appear to work while quietly having no effect on anything resolved so far.

Installing the same whitelist twice is a no-op; installing a **different** one throws
`IllegalStateException`, so a configuration that would only be applied in part fails at startup instead
of silently misleading you. `AvroClassSecurity.reset()` exists for tests only and does not evict
anything Avro has already cached.
