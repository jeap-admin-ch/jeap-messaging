# AWS MSK IAM authentication

For AWS Managed Streaming for Apache Kafka (MSK) with IAM authorization, jEAP Messaging integrates
IAM-based authentication. It requires the `jeap-messaging-aws-msk-iam-auth` module (see
[Choosing dependencies](dependencies.md)). It is independent of the Glue Schema Registry but usually
combined with it.

## Credentials

jEAP Messaging expects an `AwsCredentialsProvider` Spring bean. If none is present, it falls back to a
`DefaultCredentialsProvider`, for example the ECS task role.

## Configuration

All properties live under `jeap.messaging.kafka.cluster.<name>.aws.msk.*`.

| Name                                      | Mandatory | Default | Description                                                                        |
|-------------------------------------------|-----------|---------|------------------------------------------------------------------------------------|
| `cluster.<name>.aws.msk.iamAuthEnabled`   | Y         | `false` | Activates the MSK IAM authentication integration                                   |
| `cluster.<name>.aws.msk.region`           | N         | —       | AWS STS region; mandatory only when AssumeRole is used to authenticate against MSK |
| `cluster.<name>.aws.msk.assumeIamRoleArn` | N         | —       | ARN of a role to assume for cross-account access to MSK                            |

```yaml
jeap:
  messaging:
    kafka:
      cluster:
        default-cluster:
          aws:
            msk:
              iamAuthEnabled: true
              region: eu-central-1
```

## Related

- [AWS Glue Schema Registry](schema-registry-aws-glue.md)
- [Configuration reference](configuration.md)
- [jeap-messaging](../README.md)
