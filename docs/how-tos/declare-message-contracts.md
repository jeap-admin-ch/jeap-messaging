# Declaring message contracts

Steps to declare what your service produces and consumes, so the Message Contract Service can
document and validate it. See [Message contracts](../message-contracts.md) for why contracts exist and
how they're processed.

1. The annotations come from `jeap-messaging-contract-annotations`, normally already on the classpath
   transitively via the generated message-type JARs — no extra dependency is usually needed.
2. Add `@JeapMessageProducerContract`/`@JeapMessageConsumerContract` (or their repeatable
   `...Contracts` forms) for every message type the service produces or consumes. They can go anywhere,
   but are commonly placed on the `@SpringBootApplication` class. Each references the generated
   `TypeRef` inner class of the message type, e.g. `JmeCreateDeclarationCommand.TypeRef.class`.
3. `appName` defaults to `spring.application.name` and `topic` defaults to the message type's default
   topic — only set them to override, e.g. for a shared event consumed from several topics, or a
   contract declared on behalf of another application:

   ```java
   @JeapMessageProducerContract(JmeDeclarationCreatedEvent.TypeRef.class)
   @JeapMessageConsumerContract(JmeCreateDeclarationCommand.TypeRef.class)
   @JeapMessageConsumerContract(
           value = JmeSharedEvent.TypeRef.class,
           topic = {"my-topic", "my-topic-2"})
   @JeapMessageProducerContract(
           value = JmeAuditEvent.TypeRef.class,
           appName = "my-audit-service")
   @SpringBootApplication
   public class Application {
       public static void main(String[] args) {
           SpringApplication.run(Application.class, args);
       }
   }
   ```
4. Only if your build configures `annotationProcessorPaths` explicitly on the `maven-compiler-plugin`
   (most builds don't), add the processor path explicitly too:

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>ch.admin.bit.jeap</groupId>
                   <artifactId>jeap-messaging-contract-annotations</artifactId>
                   <version>${jeap-messaging.version}</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```
5. Build the service. The annotation processor generates the contract JSON (and the message type's
   schema) at compile time, and the jEAP build pipeline uploads both to the Message Contract Service —
   no further action is needed.

For relaxing enforcement in tests, see `publishWithoutContractAllowed` /
`consumeWithoutContractAllowed` in the [Configuration reference](../configuration.md).

## Related

- [jeap-messaging](../../README.md)
- [Message contracts](../message-contracts.md)
- [Message Type Registry](../message-type-registry.md)
- [Publishing messages](../publishing-messages.md)
- [Consuming messages](../consuming-messages.md)
