# Evolving a self-consumed message type

Steps to evolve a message type that the same service both produces and consumes, under any
compatibility mode other than `FULL`. See [Message evolution](../message-evolution.md) for why this case
needs extra care — producer and consumer of the new version are deployed in the same rolling deployment,
with no forward/backward ordering between them — and for the general forward/backward compatibility
rules.

This requires **jEAP Messaging 7.5.0+**; before that, only `FULL`-compatible changes are possible for a
self-consumed type — otherwise use EMC Message Replacement instead.

Example: removing a mandatory field from an event (a `BACKWARD`-compatible change).

1. **Declare a new version in the Message Type Registry.** Both versions can share the same event name
   but **must use different Avro namespaces**, so both can be used by the same Java runtime:

   ```text
   # version 1 — namespace ch.admin.bit.jme.test
   @namespace("ch.admin.bit.jme.test")
   protocol JmeBackwardSchemaEvolutionTestEventProtocol {
     import idl "DomainEventBaseTypes.avdl";
     import idl "ch.admin.bit.jme.test.BeanReference.avdl";

     record JmeBackwardSchemaEvolutionTestEventReferences {
       BeanReference sendingBean;
     }
     record JmeBackwardSchemaEvolutionTestEventPayload {
       string message;
       union {null, string} test = null;
     }
     record JmeBackwardSchemaEvolutionTestEvent {
       ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity identity;
       ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType type;
       ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher publisher;
       JmeBackwardSchemaEvolutionTestEventReferences references;
       JmeBackwardSchemaEvolutionTestEventPayload payload;
       union {null, string} processId = null;
       string domainEventVersion = "1.1.0";
     }
   }
   ```

   ```text
   # version 2 — namespace ch.admin.bit.jme.test.v2, the mandatory `sendingBean` reference removed
   @namespace("ch.admin.bit.jme.test.v2")
   protocol JmeBackwardSchemaEvolutionTestEventProtocol {
     import idl "DomainEventBaseTypes.avdl";

     record JmeBackwardSchemaEvolutionTestEventReferences {
     }
     record JmeBackwardSchemaEvolutionTestEventPayload {
       string message;
       union {null, string} test = null;
     }
     record JmeBackwardSchemaEvolutionTestEvent {
       ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity identity;
       ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType type;
       ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher publisher;
       JmeBackwardSchemaEvolutionTestEventReferences references;
       JmeBackwardSchemaEvolutionTestEventPayload payload;
       union {null, string} processId = null;
       string domainEventVersion = "1.1.0";
     }
   }
   ```

   The registry descriptor declares version 2's `compatibilityMode` as `BACKWARD` relative to version 1.
   Once compiled, version 1 is on the classpath as `ch.admin.bit.jme.test.JmeBackwardSchemaEvolutionTestEvent`,
   version 2 as `ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent`.

2. **Add both dependencies to `pom.xml`.** Registries using jEAP Messaging 7.5.0+ upload an additional
   artifact with a classifier equal to the version — required, or Maven treats the dependency as a
   duplicate:

   ```xml
   <dependency>
       <groupId>ch.admin.bit.jeap.messaging.test.messagetype.jme</groupId>
       <artifactId>jme-backward-schema-evolution-test-event</artifactId>
       <version>1.0.0</version>
   </dependency>
   <dependency>
       <groupId>ch.admin.bit.jeap.messaging.test.messagetype.jme</groupId>
       <artifactId>jme-backward-schema-evolution-test-event</artifactId>
       <version>2.0.0</version>
       <classifier>2.0.0</classifier>
   </dependency>
   ```

3. **Roll out one app version at a time** (App V1 → V1.1 → V1.2):
   - **App V1.1** — enhance the application to consume both V1 and V2 **before producing V2**. Force
     deserialization as V2 (the removed mandatory field is simply ignored, since the schemas are
     compatible):

     ```java
     @KafkaListener(topics = TOPIC,
             properties="specific.avro.value.type=ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent")
     public void onV2Event(ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent event, Acknowledgment ack) {
         log.info("V2 Event consumed: {}", event);
         ack.acknowledge();
     }
     ```

     Deploy. The service can now receive either version from itself or other instances during the
     rollout, and is prepared for the next step's V2 producer.
   - **App V1.2** — switch production to V2 and remove references to V1, then deploy.

   Once App V1.2 is deployed, the target state is reached: producer and consumer both use V2.

See [Configuration reference](../configuration.md) for the `specific.avro.value.type` listener property.

## Related

- [jeap-messaging](../../README.md)
- [Message evolution](../message-evolution.md)
- [Message Type Registry](../message-type-registry.md)
- [Defining messages](../defining-messages.md)
- [Configuration reference](../configuration.md)
