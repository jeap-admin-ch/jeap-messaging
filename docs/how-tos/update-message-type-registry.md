# Updating the message type registry

Steps to publish a new message type, or a new version of an existing one, in your program's
[Message Type Registry](../message-type-registry.md).

1. Check out the registry repository and create a feature branch.
2. Add or update the type descriptor (`<Type>.json`) and its Avro schema (`<type>_<version>.avdl`,
   plus an optional key schema) under `/descriptor/<system-lowercase>/event/<eventname>/` (or
   `/command/`) — see [Structure](../message-type-registry.md#structure) for the exact layout, and the
   `/schema` folder's JSON schemas if your IDE can validate against them.
3. For a new version of an existing type, append an entry to the descriptor's `versions` array and set
   its `compatibilityMode` (and `compatibleVersion`, if it isn't the immediately preceding version) —
   see [Message evolution](../message-evolution.md) for picking the right compatibility mode and migration
   strategy. Never change or remove a version already on master; add a new one instead.
4. Verify locally before opening a PR:
   ```shell
   mvn verify
   ```
   This runs the same check the build pipeline runs: every schema already on master is compared
   against your branch's version (parsed and resolved, not diffed as text) and any incompatible change
   is rejected — see [Changing existing schemas](../message-type-registry.md#changing-existing-schemas)
   for what counts as a safe vs. a rejected change.
5. Open a pull request to master. The verification pipeline must pass before merge.
6. Once merged, the registry build publishes the generated Java bindings as a Maven artifact
   (group id from the registry POM, artifact id composed of application + message name); consume it
   like any other dependency — see [Getting started](../getting-started.md) and
   [Defining messages](../defining-messages.md).

## Related

- [jeap-messaging](../../README.md)
- [Message Type Registry](../message-type-registry.md)
- [Message evolution](../message-evolution.md)
- [Defining messages](../defining-messages.md)
- [Getting started](../getting-started.md)
