# Switching to AWS Glue and MSK IAM authentication

Steps to move a cluster from the (default) Confluent Schema Registry with SASL authentication to the
AWS Glue Schema Registry with AWS MSK IAM authentication — typically when the underlying cluster itself
moves to AWS MSK. The two features are independent but usually configured together. See
[AWS Glue Schema Registry](../schema-registry-aws-glue.md) and
[AWS MSK IAM authentication](../aws-msk-iam-authentication.md) for the full property reference.

This is a per-cluster change — if the service uses [multiple clusters](../configuration.md#clusters), do
this once per cluster that actually moves, leaving the others on Confluent/SASL untouched.

1. Add the two modules (see [Choosing dependencies](../dependencies.md)):

   ```xml
   <dependency>
       <groupId>ch.admin.bit.jeap</groupId>
       <artifactId>jeap-messaging-glue-schema-registry</artifactId>
   </dependency>
   <dependency>
       <groupId>ch.admin.bit.jeap</groupId>
       <artifactId>jeap-messaging-aws-msk-iam-auth</artifactId>
   </dependency>
   ```

2. Decide on credentials. By default both sides fall back to a `DefaultCredentialsProvider` (e.g. the
   ECS task role), which is usually the right choice — for Glue this fallback is a jEAP-owned bean
   (`GlueAutoConfiguration`), while for MSK IAM auth it comes from the external `aws-msk-iam-auth`
   library itself. Only provide your own `AwsCredentialsProvider` bean, or set `assumeIamRoleArn` on the
   Glue/MSK properties below, if cross-account access is needed.
3. Point the cluster's bootstrap servers at MSK and turn on IAM authentication:

   ```yaml
   jeap:
     messaging:
       kafka:
         cluster:
           default-cluster:
             bootstrapServers: <msk-bootstrap-servers>
             aws:
               msk:
                 iamAuthEnabled: true
                 region: eu-central-1 # only required when assumeIamRoleArn is also set
   ```

4. Before switching, make sure the Confluent registry isn't active — this is checked once, globally for
   the whole service, not per cluster: `jeap.messaging.kafka.useSchemaRegistry` must not be `false` (the
   mock registry) and the legacy top-level `jeap.messaging.kafka.schemaRegistryUrl` must be unset.
   Either one being set makes the service refuse to start on **any** cluster that has
   `aws.glue.registryName` configured, with `Confluent schema registry and Glue schema registry cannot
   be active at the same time` — regardless of that cluster's own (per-cluster) Confluent properties.
   Then switch the schema registry to Glue on the cluster:

   ```yaml
             aws:
               glue:
                 registryName: my-glue-registry
                 region: eu-central-1
   ```

   `aws.glue.registryName` only activates Glue *for this cluster* — it does not disable the Confluent
   registry anywhere, automatically or otherwise; that's purely the global check above.
5. Remove the now-unused, per-cluster Confluent-specific properties on this cluster: `schemaRegistryUrl`,
   `schemaRegistryUsername`, `schemaRegistryPassword`, `username`, `password`. This is cleanup only —
   unlike step 4's global flags, these per-cluster properties don't affect whether the service starts.
   Leave `securityProtocol` alone — it defaults to `SASL_SSL`, which jEAP also uses for MSK IAM auth, so
   there's nothing to change there.
6. Nothing else needs to change: `autoRegisterSchema` keeps registering schemas automatically on first
   send, and compatibility checking stays effectively `NONE` on Glue for the same reason it is on
   Confluent — compatibility is already verified at deploy time via the Message Type Registry / Message
   Contract Service (see [Message evolution](../message-evolution.md)).
7. Roll out and verify: deploy to a non-production environment first, confirm the service starts (jEAP
   fails fast at startup only if `registryName` or `region` is missing outright — a value that's merely
   wrong, or an IAM permission problem, isn't checked eagerly and will only surface once the service
   actually tries to produce/consume) and that messages are produced/consumed as before, then promote
   the change through the usual environments.

Local development and tests are unaffected by this — Glue and MSK IAM auth aren't locally emulated, so
services typically keep using the Confluent mock registry (`useSchemaRegistry: false`) or a local
Confluent broker for dev/test, and only switch the dev/abn/prd cluster configuration that actually
points at MSK. Because the mock-registry flag is the same global, not per-cluster, switch from step 4,
the `aws.glue.registryName`/MSK properties must live only in the profiles/environments that don't set
`useSchemaRegistry: false` — a dev/test profile with the mock registry on and a Glue-configured cluster
in the same property set fails to start.

## Related

- [jeap-messaging](../../README.md)
- [AWS Glue Schema Registry](../schema-registry-aws-glue.md)
- [AWS MSK IAM authentication](../aws-msk-iam-authentication.md)
- [Confluent Schema Registry](../schema-registry-confluent.md)
- [Configuration reference](../configuration.md)
- [Choosing dependencies](../dependencies.md)
