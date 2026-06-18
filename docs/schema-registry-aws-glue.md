# AWS Glue Schema Registry

As an alternative to the Confluent Schema Registry, jEAP Messaging can use the AWS Glue Schema
Registry. It can be used independently of MSK IAM authentication, but the two are usually configured
together. It requires the `jeap-messaging-glue-schema-registry` module (see
[Choosing dependencies](dependencies.md)).

## Credentials

jEAP Messaging expects an `AwsCredentialsProvider` Spring bean. If none is present, it instantiates a
`DefaultCredentialsProvider`, which, for example on ECS, assumes the task's IAM role. This is usually a
sensible default.

## Configuration

All properties live under `jeap.messaging.kafka.cluster.<name>.aws.glue.*`.

| Name                                              | Mandatory | Default                   | Description                                                                                 |
|---------------------------------------------------|-----------|---------------------------|---------------------------------------------------------------------------------------------|
| `cluster.<name>.aws.glue.registryName`            | Y         | —                         | Name of the Glue registry; this property activates Glue and disables the Confluent registry |
| `cluster.<name>.aws.glue.region`                  | Y         | —                         | Region of the Glue registry                                                                 |
| `cluster.<name>.aws.glue.endpoint`                | N         | default regional endpoint | Override only for tests                                                                     |
| `cluster.<name>.aws.glue.assumeIamRoleArn`        | N         | —                         | ARN of a role to assume for cross-account access                                            |
| `cluster.<name>.aws.glue.stsEndpoint`             | N         | global STS endpoint       | AWS recommends the regional endpoint                                                        |
| `cluster.<name>.aws.glue.stsClientTimeoutSeconds` | N         | `30`                      | STS client timeout in seconds                                                               |

When a role is assumed, the STS role session name is derived automatically from
`spring.application.name`; it is not a separate property.

```yaml
jeap:
  messaging:
    kafka:
      cluster:
        default-cluster:
          aws:
            glue:
              registryName: my-glue-registry
              region: eu-central-1
              assumeIamRoleArn: arn:aws:iam::123456789012:role/glue-access
```

## Compatibility checking

Compatibility checking is not enabled (mode effectively `NONE`), for the same rationale as the
Confluent registry. See [Message evolution](message-evolution.md).

## Related

- [AWS MSK IAM authentication](aws-msk-iam-authentication.md)
- [Confluent Schema Registry](schema-registry-confluent.md)
- [Configuration reference](configuration.md)
- [jeap-messaging](../README.md)
