# Kafka how-to

The canonical, end-to-end checklist for wiring Kafka into a service — spanning the Message Type
Registry, the Error Handling Service and jEAP Messaging itself — now lives in the jEAP umbrella repo:
**[How to: integrate Kafka into a service](https://jeap-admin-ch.github.io/docs/how-tos/messaging/integrate-kafka-into-a-service)**.
That page also has the local-development `docker-compose.yml` (KRaft broker, schema registry,
Postgres for the error-handling service) and the acknowledge/commit and idempotent-consumer patterns.

For this library's own reference pages on the individual steps, see:

- [Getting started](../getting-started.md) — add the dependency, configure a cluster, publish and consume
- [Message Type Registry](../message-type-registry.md) — define and publish message types
- [Message contracts](../message-contracts.md) — declare what you produce/consume
- [Idempotent message handler](../idempotent-message-handler.md) — the automatic idempotent-receiver pattern
- [Error handling](../error-handling.md) — wire up an Error Handling Service
- [Kafka topics & client configuration](../kafka-topics-and-configuration.md) — topic/partition/client settings

## Related

- [jeap-messaging](../../README.md)
- [Getting started](../getting-started.md)
- [Consuming messages](../consuming-messages.md)
- [Kafka topics & client configuration](../kafka-topics-and-configuration.md)
- [Message Type Registry](../message-type-registry.md)
- [Error handling](../error-handling.md)
