# Health indicators

When `spring-boot-actuator` is on the classpath, jEAP Messaging automatically registers a health
indicator.

## `jeapKafka`

The `jeapKafka` indicator checks broker connectivity for each configured Kafka cluster by calling
`describeCluster()` via the Kafka Admin API. It reports `clusterId` and `nodeCount` per cluster and is
available at `/actuator/health/jeapKafka`.

## Probe integration

Probe integration is opt-in: `jeapKafka` can be added to the liveness and/or readiness probe groups.

```properties
management.endpoint.health.probes.enabled=true
management.endpoint.health.group.liveness.include=livenessState,jeapKafka
management.endpoint.health.group.readiness.include=readinessState,jeapKafka
```

The same `down-after` grace period applies to both.

## Recovery grace period

The indicator reports `UP` with a `recovering: true` detail for a configurable grace period after a
failure is first detected, and only flips to `DOWN` after it elapses — giving Kafka's built-in
reconnection time to recover.

The total time before a probe sees `DOWN` = `down-after` + (failure threshold × check interval). With
the default `down-after=35s`:

| Platform   | Probe settings                                       | Time before `DOWN` |
|------------|------------------------------------------------------|--------------------|
| Kubernetes | `failureThreshold=5`, `periodSeconds=5`              | ≈ 60s              |
| AWS ECS    | `unhealthy_threshold=5`, `health_check_interval=15s` | ≈ 110s             |

## Properties

| Property                                              | Default | Description                                                          |
|-------------------------------------------------------|---------|----------------------------------------------------------------------|
| `management.health.jeap-kafka.enabled`                | `true`  | Enable/disable the indicator                                         |
| `management.health.jeap-kafka.response-timeout`       | `1s`    | Timeout per broker check                                             |
| `management.health.jeap-kafka.down-after`             | `35s`   | Grace period before reporting `DOWN`                                 |
| `management.health.kafka.enabled`                     | `false` | Spring Boot's built-in single-cluster indicator, disabled by default |
| `spring.kafka.listener.auth-exception-retry-interval` | `10s`   | Retry interval for auth failures in listener containers              |

## Related

- [jeap-messaging](../README.md)
- [Configuration reference](configuration.md)
- [Kafka topics & client configuration](kafka-topics-and-configuration.md)
