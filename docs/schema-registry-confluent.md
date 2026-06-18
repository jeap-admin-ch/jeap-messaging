# Confluent Schema Registry

jEAP Messaging uses the Confluent Schema Registry by default. Schemas of sent messages are registered
automatically before the first send (`autoRegisterSchema=true`, using the `TopicRecordNameStrategy`).
Registration is largely transparent to developers.

## Compatibility mode NONE

Since 7.3.0 jEAP Messaging sets the subject's compatibility mode to `NONE` on the Confluent registry
before registering a schema. Compatibility is already verified at deploy time (can-I-deploy) via the
Message Type Registry / Message Contract Service, so no second check happens on Confluent. See
[Message evolution](message-evolution.md).

## Configuration

All properties live under `jeap.messaging.kafka.cluster.<name>.*`.

| Name                                    | Default    | Description                                                                                                                                                                   |
|-----------------------------------------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `cluster.<name>.schemaRegistryUrl`      | —          | URL of the Confluent Schema Registry. Required (and enables Confluent) when `useSchemaRegistry=true` and Glue is not configured                                               |
| `cluster.<name>.schemaRegistryUsername` | —          | Username for the registry; only if the registry requires authentication                                                                                                       |
| `cluster.<name>.schemaRegistryPassword` | —          | Password for the registry                                                                                                                                                     |
| `cluster.<name>.securityProtocol`       | `SASL_SSL` | Kafka security protocol; use `SASL_SSL` or `SASL_PLAINTEXT` for authenticated access, `SSL`/`PLAINTEXT` for unauthenticated. Defaults to `SASL_SSL` even for AWS MSK IAM auth |
| `cluster.<name>.username`               | —          | Kafka SASL username                                                                                                                                                           |
| `cluster.<name>.password`               | —          | Kafka SASL password                                                                                                                                                           |

```yaml
jeap:
  messaging:
    kafka:
      cluster:
        default-cluster:
          schemaRegistryUrl: https://schema-registry.example.ch
          securityProtocol: SASL_SSL
          username: my-service
          password: ${KAFKA_PASSWORD}
```

## Mock registry for tests

Setting `useSchemaRegistry=false` switches to an internal mock Confluent registry, which is useful in
tests. See the [Configuration reference](configuration.md).

## Related

- [Configuration reference](configuration.md)
- [Message Type Registry](message-type-registry.md)
- [Message evolution](message-evolution.md)
- [AWS Glue Schema Registry](schema-registry-aws-glue.md)
- [jeap-messaging](../README.md)
