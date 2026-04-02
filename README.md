# jEAP Messaging

jEAP Messaging is a library to handle messaging using kafka. It provides

 * A structured definition of message types using Avro Schema and message type descriptors
 * Avro serialization of message types to kafka records
 * Pre-configured Spring Kafka beans for producers and consumers
 * Transactional outbox pattern for reliable message delivery
 * Declaration of consumed/produced message types on topics using Java annotations
 * Error handling for messages that fail during processing (integration with a jeap-errorhandling-service)
 * Health indicator for Kafka broker connectivity

## Health Indicators

When `spring-boot-actuator` is on the classpath, jeap-messaging automatically registers a health indicator:

### `jeapKafka`
Checks broker connectivity for each configured Kafka cluster by calling `describeCluster()` via the Kafka
Admin API. Reports `clusterId` and `nodeCount` per cluster. Available at `/actuator/health/jeapKafka`.

### Probe integration (opt-in)
`jeapKafka` can be added to the liveness and/or readiness probe groups in `application.properties` (or yml):

```properties
management.endpoint.health.probes.enabled=true
# Restart the pod after an extended outage — retrying from a clean state may restore connectivity
management.endpoint.health.group.liveness.include=livenessState,jeapKafka
# Mark the pod as not ready while Kafka is unreachable
management.endpoint.health.group.readiness.include=readinessState,jeapKafka
```

Both can be combined. The same `down-after` grace period applies to both, giving the built-in Kafka
reconnection logic time to recover before the probe flips to `DOWN`.

### Recovery grace period
The indicator reports `UP` with a `recovering: true` detail for a configurable grace period after a failure
is first detected. Only after this period has elapsed does it flip to `DOWN`. This gives the built-in Kafka
reconnection logic time to recover before the probe reacts.

The total time before a probe sees `DOWN` is `down-after` + (failure threshold × check interval). With the
default `down-after=35s`:

| Platform    | Settings                                             | Total time before DOWN    |
|-------------|------------------------------------------------------|---------------------------|
| Kubernetes  | `failureThreshold=5`, `periodSeconds=5`              | 35s + 25s = ~60s          |
| AWS ECS     | `unhealthy_threshold=5`, `health_check_interval=15s` | 35s + 75s = ~110s         |

The relevant defaults (all overridable in `application.properties`):

| Property                                                           | Default | Description                                                           |
|--------------------------------------------------------------------|---------|-----------------------------------------------------------------------|
| `management.health.jeap-kafka.enabled`                             | `true`  | Enable/disable the Kafka broker health indicator                      |
| `management.health.jeap-kafka.response-timeout`                    | `1s`    | Timeout per broker check                                              |
| `management.health.jeap-kafka.down-after`                          | `35s`   | Grace period before reporting DOWN                                    |
| `management.health.kafka.enabled`                                  | `false` | Spring Boot's built-in single-cluster indicator (disabled by default) |
| `spring.kafka.listener.auth-exception-retry-interval`              | `10s`   | Retry interval for auth failures in listener containers               |

## Changes

This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Note

This repository is part the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
