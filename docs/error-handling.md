# Error handling

An error handler is auto-configured as part of jEAP Messaging. When processing a message throws an
exception, the handler catches it, sends a `MessageProcessingFailedEvent` to the configured error
topic, and acknowledges the **original** message so the application keeps consuming without blocking.

## The Error Handling Service

A separate Error Handling Service (the `jeap-error-handling-service` component) reads
`MessageProcessingFailedEvent` messages from the error topic, stores them, and can:

- resubmit the original message to its topic after a delay (temporary errors);
- escalate repeated temporary errors to permanent;
- create an Agir task for permanent errors so a human can intervene.

## Integration steps

- order an error topic;
- provide a user with write access to all topics the Error Handling Service must resubmit to;
- configure roles in PAMS;
- arrange Agir access;
- configure jEAP Messaging in every consuming service (`systemName`, `serviceName`, `errorTopicName`);
- run an Error Handling Service instance.

## Configuration

All properties use the prefix `jeap.messaging.kafka`.

| Name                                             | Default                      | Description                                                                                                                                         |
|--------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `errorTopicName`                                 | —                            | Error topic for `MessageProcessingFailedEvent` messages                                                                                             |
| `systemName`                                     | —                            | Name of the sending system                                                                                                                          |
| `serviceName`                                    | `${spring.application.name}` | Name of the sending service                                                                                                                         |
| `errorServiceRetryIntervalMs`                    | `5000`                       | Retry interval for sending a `MessageProcessingFailedEvent`                                                                                         |
| `errorServiceRetryAttempts`                      | `5`                          | Number of send attempts before the application terminates                                                                                           |
| `errorEventStackTraceMaxLength`                  | `7000`                       | The caught exception's stack trace is truncated to this length so the error event stays within the topic's size limit                               |
| `errorStackTraceHashEnabled`                     | `true`                       | Compute a stack-trace hash for caught exceptions (used to group recurring errors)                                                                   |
| `errorStackTraceHashDefaultExclusionPatterns`    | (framework patterns)         | Regexes for stack-trace elements (FQCN + method) to ignore when computing the hash; defaults exclude `java.base`, Spring AOP and CGLIB proxy frames |
| `errorStackTraceHashAdditionalExclusionPatterns` | (empty)                      | Additional regexes to exclude when computing the hash, on top of the defaults                                                                       |

See the [Configuration reference](configuration.md).

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
