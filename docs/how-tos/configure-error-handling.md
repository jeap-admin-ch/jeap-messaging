# Configuring error handling

jEAP Messaging auto-configures an error handler for you (see [Error handling](../error-handling.md)); this
page is the short checklist for what your service itself needs to set up so it works.

1. Set the error-handling identity and topic (see [Configuration — Core](../configuration.md#core)):

   ```yaml
   jeap:
     messaging:
       kafka:
         systemName: JME
         serviceName: ${spring.application.name}
         errorTopicName: jme-messageprocessing-failed
   ```

   `systemName` and `errorTopicName` are required; `serviceName` defaults to
   `${spring.application.name}`.
2. Make sure the service's Kafka user has permission to publish to the error topic.
3. Set up an Error Handling Service (EHS) instance for the system, if one doesn't exist yet, and follow
   its [getting-started guide](https://jeap-admin-ch.github.io/docs/building-blocks/reusable-microservices/jeap-error-handling/getting-started)
   for the remaining steps on the EHS side: ordering topics, retry permissions, roles, Agir access and
   deploying the EHS itself.
4. Tune publication retries, stack-trace truncation and stack-trace hashing if the defaults don't fit —
   see [Error reporting](../configuration.md#error-reporting).

## Related

- [jeap-messaging](../../README.md)
- [Error handling](../error-handling.md)
- [Configuration reference](../configuration.md)
- [Consuming messages](../consuming-messages.md)
