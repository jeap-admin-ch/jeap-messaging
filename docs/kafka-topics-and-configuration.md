# Kafka topics & client configuration

jeap-messaging tunes Kafka client defaults for MESSAGING (events and commands), not data streaming. It
favours durability over latency and throughput, provides at-least-once semantics, commits offsets only
after successful processing or hand-off to error handling, expects idempotent consumers and is tuned
for clusters with 3 or more nodes.

## jEAP messaging default overrides

| Scope    | Parameter                        | jeap-messaging default       | Kafka default | Why                                                                         |
|----------|----------------------------------|------------------------------|---------------|-----------------------------------------------------------------------------|
| Producer | `acks`                           | `all`                        | `1`           | Durability                                                                  |
| Consumer | `enable.auto.commit`             | `false`                      | `true`        | Commit only after success                                                   |
| Listener | `spring.kafka.listener.ack-mode` | `MANUAL`                     | `BATCH`       | Listener acks after success, error handler acks on failure                  |
| Consumer | `max.poll.records`               | `10`                         | `500`         | ~30s per poll batch for transactional event processing                      |
| Consumer | `group.id`                       | `${spring.application.name}` | —             | One group per service                                                       |
| Client   | `reconnect.backoff.max.ms`       | `5000`                       | `1000`        | Retry the broker connection at least every 5s                               |
| Listener | `auth-exception-retry-interval`  | `10s`                        | none          | Recover from transient auth failures (e.g. cert rotation) without a restart |

Recommended topic settings: `replicas` = 3 and `min.insync.replicas` = 2 — these must be set on the
topic itself.

## Topic settings

| Setting               | Meaning                                                              | Typical                 |
|-----------------------|----------------------------------------------------------------------|-------------------------|
| Replicas              | Copies of the data including the leader; durability and availability | 3                       |
| Retention             | How long records are kept                                            | dev/ref 7d, abn/prd 28d |
| Partitions            | Limit consumer concurrency — one partition per consumer in a group   | per throughput          |
| Users                 | Access per protection need                                           | per topic               |
| `min.insync.replicas` | With `acks=all`, the minimum replicas that must ack a write          | 2                       |
| `message.max.bytes`   | Maximum record size; Kafka is not for large records                  | ~1MB                    |

For large payloads, send a reference that is retrievable via S3 or HTTP instead of the payload itself.

## Partitioning and keys

Kafka records are key/value pairs with an optional key. The key controls partition distribution via
hashing; with no key, records spread evenly across partitions. Records with the same key are consumed
in order — ordering is the main reason to set an explicit key. Keys should be fine-grained (for
example a business id) to avoid partition imbalance. Do NOT use a key unless ordering guarantees are
required. Prefer one message type per topic for maximum decoupling.

## Client configuration

Set client tuning via Spring's `spring.kafka.*` properties:

- `spring.kafka.producer.properties.*`
- `spring.kafka.consumer.properties.*`
- `spring.kafka.properties.*`

## Background

### Replication

Each partition has a leader and follower replicas. Writes go to the leader and are replicated to the
followers; the in-sync replicas are those caught up with the leader.

### Consumer groups

Partitions are distributed across the consumers of a group. The maximum number of parallel consumers
equals the number of partitions.

### Rebalancing

A rebalance happens when a consumer joins or leaves, or on a metadata change. It pauses consumption
while partitions are reassigned.

### Ordering caveats

Error-handler resubmits and `max.in.flight.requests.per.connection` > 1 (default `5`) can reorder
records on retry. This is another reason an idempotent consumer is important.

## Related

- [jeap-messaging](../README.md)
- [Consuming messages](consuming-messages.md)
- [Kafka how-to](kafka-how-to.md)
- [Configuration reference](configuration.md)
