# Error handling

An error handler is auto-configured as part of jEAP Messaging. When processing a message throws an
exception, the handler catches it, sends a `MessageProcessingFailedEvent` to the configured error
topic, and acknowledges the **original** message so the application keeps consuming without blocking.

## The Error Handling Service

The [Error Handling Service](https://jeap-admin-ch.github.io/docs/building-blocks/reusable-microservices/jeap-error-handling/) consumes the failure events and manages
retries and manual resolution. Its deployment and operation are documented with the service.

## Integration steps

Configure each consuming application with an error topic and permission to publish to it, plus its
system and service identity (see [Configuration](configuration.md#core)). For ordering topics, retry
permissions, roles, Agir access and deploying the EHS, follow the
[EHS getting-started guide](https://jeap-admin-ch.github.io/docs/building-blocks/reusable-microservices/jeap-error-handling/getting-started).

## Configuration

Properties and defaults are maintained in the [Configuration reference](configuration.md#core).
See [Error reporting](configuration.md#error-reporting) for error-publication retries, stack-trace
truncation and hashing options.

## Message listener and acknowledgment

The auto-configured handler supports record listeners, including records fetched together in one
Kafka poll. Batch listeners that process multiple records in one invocation are not supported.
For a listener example and the acknowledgment rules, see [Consuming messages](consuming-messages.md#a-listener-method).

The handler uses Spring Kafka's `DefaultErrorHandler` without local retries of business processing.
After forwarding the failed record, it seeks the remaining unprocessed records so they can be
redelivered, including when a poll contains records from several topics or partitions.

Sending the failure event has its own retries. If publication still fails when those retries are
exhausted, the default failure handler terminates the application instance without acknowledging the
failed record, allowing the container platform to restart it. These publication retries are separate
from the EHS resubmitting the original business message.

## Temporary exceptions

The error service sender automatically marks known transient framework errors as `TEMPORARY`, so the
Error Handling Service can retry them. This includes retryable Kafka errors, transient and recoverable
Spring data-access errors, transaction timeouts, common Hibernate connection/lock/timeout errors and
Spring `ResourceAccessException`s (including subclasses). The default resolver checks only the exception
passed to it, not its cause chain. An explicit `PERMANENT` or `TEMPORARY` temporality declared by that
exception through `MessageHandlerExceptionInformation` always takes precedence.

The one exception is a database failover with the AWS Advanced JDBC Wrapper: an `SQLException` with
SQLState `08S02` (`FailoverSuccessSQLException`: the connection has been re-established after the
failover, but the interrupted operation was not replayed) is classified as `TEMPORARY` anywhere in the cause chain, as Spring and Hibernate
translate it, for example to a `DataAccessResourceFailureException`, and applications usually wrap it.
The search stops at a wrapped exception that declares an explicit `PERMANENT` or `TEMPORARY` temporality.
Other connection failures, such as `08007` where the outcome of the interrupted transaction is unknown,
are not classified.

Applications can provide error code, message, description and temporality through
`MessageHandlerExceptionInformation`, or use the supplied `MessageHandlerException` implementation.

Applications can customize this classification by providing a Spring bean of type
`ExceptionTemporalityResolver`. An application-provided resolver **replaces** the default resolver, so
delegate to `DefaultExceptionTemporalityResolver` to keep the jEAP defaults:

```java
@Bean
ExceptionTemporalityResolver exceptionTemporalityResolver() {
    var defaults = new DefaultExceptionTemporalityResolver();
    return exception -> exception instanceof MyTemporaryException
            ? Optional.of(MessageHandlerExceptionInformation.Temporality.TEMPORARY)
            : defaults.resolve(exception);
}
```

Returning an empty `Optional` keeps the error `UNKNOWN` for message-processing exceptions. Serialization
failures default to `PERMANENT` only when neither the resolver nor the deserialization cause provides
temporality information. If that cause implements `MessageHandlerExceptionInformation`, its temporality
is preserved as before, including `UNKNOWN`, unless the resolver returns a concrete classification.
Explicit `PERMANENT` or `TEMPORARY` values supplied through `MessageHandlerExceptionInformation` by the
thrown exception itself remain unchanged; only its temporality is replaced if it is `UNKNOWN` — error
code, message, description and stack trace are reported as provided.

## Error stack trace hashing

Since version 8.7.0, the error handler can include a stack-trace hash in the failure event. The EHS
uses matching hashes to group recurring errors. Exclude variable stack frames to keep the grouping
stable: exclusion patterns are Java regular expressions matched against a fully qualified class name
with its method name, for example `ch.admin.bit.mypackage.MyClass.myMethod`.

Defaults and examples for replacing or extending the exclusion patterns are in
[Error reporting configuration](configuration.md#error-reporting).

## Example

The [jme-messaging-example](https://github.com/jme-admin-ch/jme-messaging-example) demonstrates the
integration. Its subscriber throws different exceptions depending on the message content; see
[ExampleController.java](https://github.com/jme-admin-ch/jme-messaging-example/blob/main/jme-messaging-subscriber-service/src/main/java/ch/admin/bit/jeap/jme/messaging/receiver/ExampleController.java).
The [jme-messaging-error-scs](https://github.com/jme-admin-ch/jme-messaging-example/tree/main/jme-messaging-error-scs)
provides the EHS instance.

## Guidelines

- every message-receiving application must have an Error Handling Service;
- the library's error handler must be used; if you override it, you must guarantee that no message is
  lost;
- unprocessable messages must be written to the error topic as `MessageProcessingFailedEvent` messages — this
  is automatic when using jEAP Messaging.

Resubmitted messages are automatically filtered to the intended consumer — see
[Message filtering](message-filtering.md).

## Related

- [jeap-messaging](../README.md)
- [Consuming messages](consuming-messages.md)
- [Message filtering](message-filtering.md)
- [Configuration reference](configuration.md)
