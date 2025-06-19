# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [8.47.1] - 2025-06-19

### Fixed
- Fix bug in message signing verifier, where certificate common and service name were twisted 

## [8.47.0] - 2025-06-18

### Changed
- Update parent from 5.10.1 to 5.10.2
- update jeap-spring-boot-vault-starter.version from 17.37.0 to 17.38.0
- update jeap-crypto.version from 3.22.1 to 3.23.0

## [8.46.0] - 2025-06-18

### Changed
- Overwrite commons-io version (2.11.0) from spring-kafka-test 3.3.6 with 2.19.0 (CVE-2024-47554)
- Overwrite commons-beanutils version (1.9.4) from spring-kafka-test 3.3.6 with 1.11.0 (CVE-2025-48734)

## [8.45.0] - 2025-06-17

### Changed
- Update parent from 5.10.0 to 5.10.1
- Update because to upload (central-publish) didn't work properly
- update jeap-crypto.version from 3.21.0 to 3.22.1

## [8.44.0] - 2025-06-13

### Changed
- Update parent from 5.9.0 to 5.10.0
- update jeap-spring-boot-vault-starter.version from 17.35.0 to 17.36.0
- update jeap-crypto.version from 3.20.0 to 3.21.0

## [8.43.0] - 2025-06-13
### Changed
Moved jeap-messaging-outbox to its own repository.
Moved jeap-messaging-sequential-inbox to its own repository.

## [8.42.0] - 2025-06-12
### Changed
- update jeap-crypto.version from 3.19.0 to 3.20.0
- update jeap-spring-boot-vault-starter.version from 17.34.0 to 17.35.0
- security-starter-test: removed spring-security-rsa dependency as its functionality is now included in spring-security


## [8.41.0] - 2025-06-10

### Changed
- The main branch name used by the MessageTypeRegistryVerifier plugin is now configurable. Defaults to 'master'.

## [8.40.0] - 2025-06-05

### Changed
- Update parent from 5.8.1 to 5.9.0
- update jeap-crypto.version from 3.17.0 to 3.18.0
- Update parent from 5.8.1 to 5.9.0
- Project Name now required for uploads to Maven Central
- update jeap-crypto.version from 3.18.0 to 3.19.0
- update jeap-spring-boot-vault-starter.version from 17.32.0 to 17.34.0
- Update parent from 5.8.1 to 5.9.0

## [8.39.0] - 2025-06-04

### Changed
- SequentialInbox: Prevent parallel execution of housekeeping methods using SchedulerLock. Ensure the shedlock table exists if ShedLock is not already configured.

## [8.38.1] - 2025-05-26

### Changed
- some corrections because SONAR was complaining.

## [8.38.0] - 2025-05-26

### Changed
- update jeap-crypto.version from 3.15.0 to 3.17.0
- Update parent from 5.8.0 to 5.8.1

## [8.37.0] - 2025-05-14

### Changed

- Add topic name to JeapKafkaMessageCallback methods

## [8.36.0] - 2025-05-01

### Changed

- Invoke JeapKafkaMessageCallback methods when using transactions outbox / sequential inbox

## [8.35.0] - 2025-04-30

### Changed

- Update jeap-crypto from 3.14.0 to 3.15.0
- Update aws-msk-iam-auth from 2.3.1 to 2.3.2
- Update file-management from 3.1.0 to 3.2.0

## [8.34.0] - 2025-04-30

### Changed

- Update parent from 5.7.0 to 5.8.0

## [8.33.0] - 2025-04-24

### Added

- Add JeapKafkaMessageCallback for clients interested in notifications on message send and consume 

## [8.32.0] - 2025-04-23

### Changed

- Add contract exemptions for ReactionIdentifiedEvent and ReactionsObservedEvent
- Remove deprecated / unnecessary contract exemptions for process context service events

## [8.31.2] - 2025-04-03

### Changed

- More logging information for signature subscriber certificates

## [8.31.1] - 2025-04-01

### Changed

- Avoid logging constraint violation exception when two sequence instances are created at the same time

## [8.31.0] - 2025-03-31

### Changed

- Update parent from 5.6.1 to 5.7.0
- Update jeap-crypto from 3.13.0 to 3.14.0
- Update avro-serializer from 7.7.2 to 7.9.0
- Update aws-msk-iam-auth from 2.3.0 to 2.3.1
- Update protobuf-java from 4.29.0 to 4.30.2
- Update org.eclipse.jgit.version from 7.1.0.202411261347-r to 7.2.0.202503040940-r
- Update maven-invoker from 3.2.0 to 3.3.0

## [8.30.3] - 2025-03-28

### Fixed

- Message Contract Annotation Processor: Avoid creating duplicate contracts for the same message type / topic combination
  when generating contracts from templates. 

## [8.30.2] - 2025-03-26

### Fixed

- Sequential Inbox: Fix naming of metrics and remove histogram config for the handle-message metric

## [8.30.1] - 2025-03-26

### Fixed

- Removed unique constraint definition in entity SequenceInstance

## [8.30.0] - 2025-03-20

### Added

- The sequential inbox configuration now supports defining subtypes
  - This allows to define a sequence for different subtypes of the same avro message type

## [8.29.0] - 2025-03-18

### Changed

- Improved: The `Pessimistic Lock` around the inbox for a context no longer blocks Kafka partitions containing records that could already be processed.
- Processing or buffering these records can now occur before the `Pessimistic Lock`.

## [8.28.0] - 2025-03-14

### Changed

- Sequential Inbox: add support for encrypted and signed messages: store the specific headers of the consumed records in
  the database in order to reset these headers when serializing the stored sequenced messages.

## [8.27.2] - 2025-03-14

### Fixed

- Fixed sequential inbox record mode integration tests.

## [8.27.1] - 2025-03-13

### Fixed

- Read SequentialInbox configuration as InputStream so that it also works on RHOS

## [8.27.0] - 2025-03-12

### Changed

- The sequencing start timestamp can be set to define when the sequencing should begin. This can be configured using the optional property `jeap.messaging.sequential-inbox.sequencing-start-timestamp`.
- This change is necessary to handle messages that need to be newly sequenced, especially when their predecessors were received before the introduction of the sequence.
- If this property is not configured, the sequencing will start immediately. Messages with an already received predecessors can then not be processed. 

## [8.26.0] - 2025-03-06

### Changed

- Add message signing benchmarks
- Minor bugfixes in message signing
- Removed conflicting versions of the Amazon Corretto Crypto Provider (ACCP) that could lead to the ACCP being disabled

## [8.25.0] - 2025-03-06

### Changed

- Update parent from 5.5.5 to 5.6.0
- Update jeap-crypto from 3.11.0 to 3.12.0
- Update aws-msk-iam-auth from 2.2.0 to 2.3.0
- Update schema-registry-serde from 1.1.22 to 1.1.23

## [8.24.0] - 2025-03-05

### Changed

- Add metrics for the sequential inbox
  - jeap.messaging.sequential-inbox.metrics.waiting-messages counts the number of messages waiting to be processed (per messagetype)
  - jeap.messaging.sequential-inbox.metrics.consumed-messages counts the number of messages consumed in total (per messagetype)
  - jeap.messaging.sequential-inbox.metrics.waiting-message-delay measures the delay between the message being buffered and the message being processed (per messagetype)

## [8.23.0] - 2025-03-05

### Changed

- Update parent from 5.5.4 to 5.5.5

## [8.22.0] - 2025-03-04

### Added

- Add sequential inbox: provides a way to consume messages in a defined sequential order

## [8.21.0] - 2025-02-25

### Changed

- Add support for the validation of signed kafka record values and keys.
- Update parent from 5.5.3 to 5.5.4

## [8.20.0] - 2025-02-18

### Changed

- Preserve Signature headers for EHS (Error Handling Service).

## [8.19.0] - 2025-02-14

### Changed

- Add support for signing kafka record values and keys in outbox.

## [8.18.0] - 2025-02-13

### Changed

- Add support for signing kafka record values and keys.


## [8.17.0] - 2025-02-11

### Changed

- Add support for custom messagetype pom templates in the jeap-messaging-avro-maven-plugin
- Add configuration option for maven profile when publishing messagetypes on the trunk 
- Pass proxy properties to invoked maven instance when publishing messagetypes

## [8.16.0] - 2025-02-10

### Changed

- Update parent from 5.5.0 to 5.5.1

## [8.15.0] - 2025-02-07

### Added

- Update jeap-internal-spring-boot-parent to 5.5.0 (spring boot 3.4.2)
- Publish to maven central

## [8.14.0] - 2025-01-27

### Added

- Added possibility to find unused imports in IDL (Avro) files.

## [8.13.2] - 2025-01-10

### Added

- Added method to ensure a consumer contract just by the message type name in the ContractValidator interface.


## [8.13.1] - 2025-01-07

### Added

- Added new template path to annotation processor for automated consumer contract generation by json template files via @JeapMessageConsumerContractsByTemplates.

## [8.13.0] - 2024-12-20

### Added

- Implemented annotation processor for automated consumer contract generation by json template files.
- Added @JeapMessageConsumerContractsByTemplates 

## [8.12.0] - 2024-12-31

### Changed

- Update parent from 5.4.0 to 5.4.1

## [8.11.0] - 2024-12-18

### Changed

- upgrade spring boot to version 3.4.0

## [8.10.1] - 2024-12-17

### Added

- credential scan (trufflehog)

## [8.10.0] - 2024-12-09

### Changed

- Configure trivy scan for all branches

## [8.9.0] - 2024-12-06

### Changed

- Update parent from 5.2.5 to 5.3.0
- Prepare repository for Open Source distribution

## [8.8.2] - 2024-12-03

### Changed

- Improved Kafka embedded tests to be more stable

## [8.8.1] - 2024-11-21

### Changed

- Restricted the sizes on the composite primary key fields in the idempotent_processing table to comply with
  requirements of the AWS Database Migration Service.

## [8.8.0] - 2024-11-12

### Changed

- Prepare repository for Open Source distribution

## [8.7.0] - 2024-11-12

### Changed

- Added a stack trace hash to the message processing failed event.

## [8.6.0] - 2024-11-12

### Added

- License definition & license plugins

### Changed

- Moved avro compiler and validator classes into separate modules to avoid dependencies on the maven plugin to re-use
  the classes in other modules / services

## [8.5.0] - 2024-11-08

### Changed

- Update dependencies
  - org.codehaus.mojo:build-helper-maven-plugin from 3.0.0 to 3.6.0
  - org.apache.maven.shared:maven-invoker from 3.2.0 to 3.3.0
  - org.apache.commons:commons-text from 1.9 to 1.12
  - org.apache.maven.plugin-tools:maven-plugin-annotations from 3.6.0 to 3.15.1
  - org.junit-pioneer:junit-pioneer from 2.0.1 to 2.3.0
  - com.google.jimfs:jimfs from 1.2 to 1.3.0
  - net.logstash.logback:logstash-logback-encoder from 5.2 to 5.3
  - org.awaitility:awaitility from 4.2.0 to 4.2.2
  - com.github.java-json-tools:json-schema-validator from 2.2.12 to 2.2.14
  - org.skyscreamer:jsonassert from 1.5.0 to 1.5.3
  - jeap-crypto from 3.1.0 to 3.3.0
  - maven.api from 3.6.2 to 3.9.9
  - aws-msk-iam-auth from 2.1.1 to 2.2.0
  - schema-registry-serde from 1.1.20 to 1.1.21
  - org.eclipse.jgit from 6.0.0.202111291000-r to 6.10.0.202406032230-r
  - org.apache.commons:commons-compress from 1.26.1 to 1.27.1
  - removed com.github.tomakehurst

## [8.4.0] - 2024-11-07

### Changed

- Update parent from 5.1.1 to 5.2.1

## [8.3.0] - 2024-10-31

### Changed

- Update parent from 5.1.0 to 5.1.1

## [8.2.1] - 2024-10-24

- The transactional outbox's message relay process now relays messages persisted for a no longer configured cluster to
  the default cluster while respecting a default producer cluster override in such cases if configured.

## [8.2.0] - 2024-10-16

### Changed

- Update avro to 1.12.0, avro-serializer to 7.7.1 (fixes CVE-2024-47561)

## [8.1.1] - 2024-09-23

### Changed

- Update com.google.protobuf:protobuf-java from 3.19.6 to 3.25.5 to fix CVE-2024-7254

## [8.1.0] - 2024-09-20

### Changed

- Update parent from 5.0.0 to 5.1.0
- Update jeap-crypto from 3.0.0 to 3.1.0

## [8.0.0] - 2024-09-06

### Changed

- Update parent from 4.11.1 to 5.0.0 (java 21)
- Update jeap-crypto from 2.7.0 to 3.0.0 (java 21)

## [7.22.0] - 2024-08-29

### Changed

- Add a transactionId to contract-maven-plugin to identify uploads of the same transaction

## [7.21.0] - 2024-08-29

### Changed

- Added an optional user field to the jEAP message interface and the avro definitions.
- Added logging of the user id for incoming and outgoing messages.

## [7.20.0] - 2024-08-21

### Changed

- Update parent from 4.10.0 to 4.11.1

## [7.19.0] - 2024-07-17

### Changed

- remove json-schema-validator dependency, because it's not used anymore

## [7.18.0] - 2024-07-16

### Changed

- Update org.apache.maven.shared file-management version in order to avoid scan problems

## [7.17.0] - 2024-07-15

### Changed

- Update parent from 4.8.3 to 4.10.0

## [7.16.1] - 2024-07-03

### Changed

- little correction in the handling of truststore parameters

## [7.16.0] - 2024-07-02

### Changed

- Support for mutual authentication (ssl), this is needed on RHOS

## [7.15.0] - 2024-06-28

### Changed

- Transactional outbox: change the default retention duration of sent items from 7 to 2 days
- Transactional outbox: add new metric outbox_messages_ready_to_be_sent

## [7.14.0] - 2024-06-24

### Changed

- Avro-Maven-Plugin: the property enableDecimalLogicalType of avro compiler is configurable

## [7.13.0] - 2024-05-16

### Changed

- Changed the @TestKafkaListener to use its own consumer group id by default: ${spring.application.name:testapp}-test.

## [7.12.1] - 2024-05-03

### Changed

- Update parent from 4.8.2 to 4.8.3

## [7.11.0] - 2024-04-26

### Changed

- registry-maven-plugin: for the jeap system, the system prefix in message names is not mandatory

## [7.10.0] - 2024-04-02

### Added

- Added property jeap.messaging.kafka.cluster.producer.default-producer-cluster-override
  to mark a kafka cluster as the default cluster to produce messages to.

## [7.9.2] - 2024-03-28

### Changed

- Fixed a problem that would avoid starting applications when no BraveAutoConfiguration was found in classpath

## [7.9.1] - 2024-03-27

### Changed

- Manage version of commons-compress to use latest version without CVEs

## [7.9.0] - 2024-03-27

### Changed

- Update parent from 4.8.0 to 4.8.2 (Spring Boot 3.2.4)

## [7.8.1] - 2024-03-25

- Under some circumstances tracing for Kafka listener wasn't working

## [7.8.0] - 2024-03-25

- MessageProcessingFailedEvent: the stackTrace in payload is truncated if it is larger than the maximum length defined

## [7.7.1] - 2024-03-18

- Throwing more specific exceptions in the idempotent message handler.

## [7.7.0] - 2024-03-04

- Upgrading to jeap spring boot parent 4.8.0 (spring boot 3.2.3, management of bcprov-jdk18on dependency)
- Upgrading from bcprov-jdk15on to bcprov-jdk18on for the legacy message encryption feature

## [7.6.1] - 2024-02-29

- Added more integration tests for self-message case

## [7.6.0] - 2024-02-27

- Message deserializers (AWS Glue) are now instantiated for each message listener
- Target type in deserializers can be overridden

## [7.5.0] - 2024-02-26

- jEAP messaging Avro Maven plugin now generates additional artifacts with classifiers to enable message evolution for
  messages sent to same service

## [7.4.0] - 2024-02-20

- Make jEAP messaging infrastructure beans / interceptors work more seamlessly with non-jEAP avro messages

## [7.3.0] - 2024-02-19

- The compatibility mode in the message type descriptor is defined and validated for each message type version

## [7.2.0] - 2024-02-15

- Message deserializers are now instantiated for each message listener
- Target type in deserializers can be overridden

## [7.1.0] - 2024-02-15

- Update encryption / decryption to be able to handle multiple key management system technologies

## [7.0.1] - 2024-02-09

- Fix git tags comparison in avro-maven-plugin for the message-type-registry

## [7.0.0] - 2024-02-02

- Removed support for v1 messaging contracts

## [6.5.0] - 2024-01-25

- Upgraded jeap-internal-spring-boot-parent from 4.4.1 to 4.5.0 (spring boot 3.2.2)

## [6.4.1] - 22.01.2024

- Remove leftover properties in embedded kafka defaults

## [6.4.0] - 16.01.2024

- Upgrade internal parent to 4.4.0

## [6.3.2] - 05.01.2024

- Exclude swagger annotations dependency from confluent schema registry client due to conflicts
- Upgrade internal parent to 4.3.2

## [6.3.1] - 28.12.2023

- Make KafkaProperties safe to use when refreshing properties
- Allow mixing legacy and multi-cluster kakfa properties for migration use cases

## [6.3.0] - 21.12.2023

- Upgrade avro to 1.11.3, avro-serializer to 7.5.1

## [6.2.1] - 15.12.2023

- Embedded kafka properties take precedence if embedded kafka is active

## [6.2.0] - 14.12.2023

- Upgrade jeap-internal-spring-boot-parent from 4.0.0 to 4.3.0 (spring boot 3.2)

## [6.1.0] - 13.12.2023

- Added basic support for reactive Kafka (multi-cluster)

## [6.0.6] - 08.12.2023

- Set kafka admin instance for cluster on KafkaTemplate

## [6.0.5] - 04.12.2023

- Make SSL properties for Kafka connection optional for AWS as they are not required

## [6.0.4] - 04.12.2023

- Fix commons-logging dependency conflict with spring-jcl

## [6.0.3] - 01.12.2023

- Enable observation also for non-default cluster kafka templates

## [6.0.2] - 30.11.2023

- Add genericDataRecordDeserializer to KafkaAvroSerdeProvider

## [6.0.1] - 22.11.2023

- Fix KafkaAdmin configuration for non-default cluster

## [6.0.0] - 22.11.2023

- Support configuring more than one kafka cluster connection
- Replace EmbeddedKafkaProperties with automated Embedded Kafka detection
- Add column "cluster_name" to jEAP messaging outbox. Use this script to update your schema:
  `ALTER TABLE deferred_message ADD COLUMN cluster_name varchar;`

## [5.1.2] - 17.11.2023

- Consider multiple classpath locations when looking for message contracts

## [5.1.1] - 08.09.2023

- Import EmbeddedKafkaProperties in KafkaIntegrationTestBase as it is always required for embedded kafka tests

## [5.1.0] - 22.08.2023

- Avro Compiler Maven Plugin: Detect changes to message types also when schema is not changed
- Avro Registry Maven Plugin: Detect dangling schema files

## [5.0.0] - 16.08.2023

- Upgrade internal parent to 4.0.0 (spring boot 3.1)

## [4.14.6] - 07.08.2023

- Fix overly strict consumer contract validation: Ignore consumed version

## [4.14.5] - 18.07.2023

- Added trigger for automatic upgrade of jeap-spring-boot-parent

## [4.14.4] - 13.07.2023

- Fix validation of message type definition in schemas: Now takes correct version into account

## [4.14.3] - 05.07.2023

- AWS MSK / Glue logging and property validation improvements
- Make sure to differentiate key and value schema per topic for AWS Glue

## [4.14.2] - 30.06.2023

- Setting a region for AWS MSK is now only mandatory when using assume-role authentication

## [4.14.1] - 29.06.2023

- Fixed: The security protocol value for AWS MSK is now set as a String value and not as an enum value

## [4.14.0] - 28.06.2023

- retrieve and store traceIdHigh from traceContext to support traceId128
- When using the transactional outbox, you MUST ADD these columns to the `deferred_message` table:
  * ```ALTER TABLE deferred_message ADD column trace_id_string varchar;```
  * ```ALTER TABLE deferred_message ADD column trace_id_high bigint;```

## [4.13.1] - 23.06.2023

- Use dependency management for AWS SDK from internal parent

## [4.13.0] - 21.06.2023

- Add support for AWS Glue schema registry
- Add support for AWS MSK IAM Authentication

## [4.12.0] - 15.06.2023

- Internal refactoring: Separate confluent schema registry specific code into separate module
  - Note: Some internal configuration classes have been moved, i.e. CustomKafkaAvro* are now located under
    ch.admin.bit.jeap.messaging.kafka.serde.confluent

## [4.11.2] - 30.05.2023

- Fixed missing default value for kafka bootstrap servers in integration tests without embedded kafka

## [4.11.1] - 23.05.2023

- Fixed a possible null pointer exception in MessageProcessingFailedEventBuilder.getSerializedMessage(..) that could
  prevent the sending of a MessageProcessingFailedEvent if the original message's key was deserialized to an
  EmptyMessageKey with a null message.

## [4.11.0] - 10.05.2023

### Changed

- Include metadata about the original message in the MessageProcessingFailedEvent to avoid the need to deserialize it in
  the error handling service
- Use random port for embedded kafka tests to avoid test interference

## [4.10.0] - 10.05.2023

Non-public version, encryption flag header is missing in MessageProcessingFailedEvent

## [4.9.0] - 26.04.2023

### Added

- Support for encryption and decryption of messages using the jeap crypto library.

## [4.8.3] - 20.04.2023

### Changed

- Upgrade internal parent to 3.4.1 (spring boot 2.7.11)

## [4.8.2] - 05.04.2023

### Changed

- Publish sources for generated message types

## [4.8.1] - 30.03.2023

### Changed

- Detect message contract app name indepent of current working directory

## [4.8.0] - 27.03.2023

### Changed

- Schema validation follows the message type hierarchy described
  - Type (Record) recognition still works on a naming convention-basis, i.e. for Records ending with 'Reference' or 
    'Event', etc.
  - With the new behavior, Records ending with Reference or References will only be validated when they belong to a
    DomainEvent or Command.

## [4.7.1] - 07.03.2023

### Changed

- Default lifecycle phase to 'none' for the publish-messaging-contracts mojo to activate it using direct execution

## [4.7.0] - 03.03.2023

### Changed

- Registry-Maven-Plugin:
  - it is no longer possible to modify existing definitions in the descriptor
  - it is now possible to add a new version of a message defining a key even if the first versions did not define a key

- IdempotentMessageHandler:
  - ignore major version in IdempotentProcessingIdentity
  - if the microservice already consumes messages containing a version number, this data must be migrated using a SQL
    script with Flyway (table idempotent_processing, column idempotence_id_context)

## [4.6.0] - 20.02.2023

### Changed

- Upgrading to Java 17
- Upgrading dependency versions (e.g. jeap-internal-parent to 3.4.0)

## [4.5.4] - 17.02.2023

### Changed

- Revert log volume reduction concerning consumer and producer interceptors

## [4.5.3] - 03.02.2023

### Changed

- Reduce log volume on level info
- Support multiple topic names in message type descriptor

## [4.5.2] - 12.01.2023

### Changed

- For performance reasons, remove Spring Data queries in favor of native queries for batch message deletion. 

## [4.5.1] - 20.12.2022

### Added

- Support for finding the spring boot application name for contract annotations in sibling modules 

## [4.5.0] - 12.12.2022

### Added

- Support for producing/consuming messages without contract check in integration tests

## [4.4.2] - 08.12.2022

### Changed

- Fix null pointer in generate contracts if default topic is null
- Fix defintion of JeapMessageAnnotationProcessor (add container annotations to process multiple annotations)

## [4.4.1] - 07.12.2022

### Changed

- Fix version in generated pom for common message types

## [4.4.0] - 07.12.2022

### Changed

- Avro Maven Plugin
  - Support parallelization of message type upload to maven repository
  - Made trunk branch name configurable
  - Support skipping message type compilation
  - Reference the latest common type dependency

## [4.3.3] - 29.11.2022

### Changed

- Fixed hard dependency on MeterRegistry class in ContractConfig

## [4.3.2] - 29.11.2022

### Changed

- Made DefaultContractsValidator public to allow for re-use in process context/archive services

## [4.3.1] - 17.11.2022

### Fixed

- Fixed spring boot app name detection in contracts annotation processor

## [4.3.0] - 16.11.2022

### Added

- Added publisher and consumer contract validation support for v2 contracts. 

## [4.2.0] - 05.11.2022

### Added

- Added options to configure different kafka bootstrap servers for consumers, producers and admin clients.

## [4.1.3] - 03.11.2022

### Fixed

- Spring boot app name in dot notation is detected correctly in contract annotation processor

## [4.1.2] - 31.10.2022

### Fixed

- Removed @EnableTransactionManagement annotation from the autoconfigurations for the idempotent message handler
  annotation
  and for the transactional outbox as those annotations could interfere with the same annotation provided by the
  application if
  the application specified an order parameter.

## [4.1.1] - 21.10.2022

### Fixed

- Fixed '"traceContext" is null' when sending a message with the transactional outbox if the transaction manager has not
  been
  instrumented by the brave tracing library (usually happens only in unit tests).

## [4.1.0] - 14.10.2022

### Added

- jeap-messaging-contract-maven-plugin: Uploads jeap message contracts to the message contract service

## [4.0.1] - 12.10.2022

### Fixed

- Avro-Maven-Plugin registry generates the java source files in the same directory and no more in separated directories

## [4.0.0] - 04.10.2022

### Changed

- New goals in Avro-Maven-Plugin to generate the message types from the registry and to publish the files
- Avro-Maven-Plugin generates java source files in separated directories per MessageType
- Generated message type avro bindings contain type metadata (TypeRef) if retrieved from a message type registry
- Replaced Spring Kafka SeekToCurrentErrorHandler (deprecated) with DefaultErrorHandler
- Add KafkaConsumerPropertiesValidator to validate if error handling properties are set when consumers are present
- Add jeap-messaging-contract-annotations
- Removed deprecated EventHandlerException (replaced by MessageHandlerException)
- Use causing event ID as key when producing a MessageProcessingFailedEvent
- Moved KafkaMockTestConfig to the new package 'mockkafka' to avoid exceptions if projects are using component scan on the 'kafka' package
- Upgraded to jeap internal parent 3.3.0 (spring boot 2.7)

### Fixed
 
- Fixed possible race conditions in tests.
- Add further changes...

## [3.9.1] - 28.06.2022

### Fixed

- Convert KafkaMockTestConfig to KafkaMockTestBase to avoid exceptions during integration tests

## [3.9.0] - 02.06.2022

### Added

- Add version label to message consumer/producer metrics
- Added messageTypeVersion attribute to deferred messages
- When using the transactional outbox, you MUST ADD this column to the `deferred_message` table:
  ```ALTER TABLE deferred_message ADD column message_type_version varchar;```

## [3.8.0] - 24.05.2022

### Added

- Added KafkaMockTestConfig to run integration tests without kafka

## [3.7.1] - 18.05.2022

### Fixed

- Type bounds on AvroMessageBuilder were too strict and led to compile errors with custom abstract message builders

## [3.7.0] - 16.05.2022

### Changed

- Generated Avro bindings for jEAP messages automatically provide the message type version. Setting a (possibly)
  outdated message version manually is being deprecated.

## [3.6.0] - 11.04.2022

### Added

- Added CreateProcessInstanceCommand to contracts exemption list in DefaultContractsProvider

## [3.5.2] - 04.04.2022

### Changed

- Upgraded internal parent to 2.3.3 (spring boot 2.6.6)

## [3.5.1] - 29.03.2022

### Changed

- Upgraded internal parent to 2.3.1 (spring boot 2.6.4)

## [3.5.0] - 09.02.2022

### Added

- Added ProducerMetricsInterceptor and ConsumerMetricsInterceptor to provide metrics on consuming and sending messages.

## [3.4.2] - 01.02.2022

### Fixed

Fixed Kafka serializer configuration incorrectly applied to jEAP messaging Kafka message and key serializer beans.

## [3.4.1] - 17.01.2022

### Bugfix
- In the Avro-Schema, it's now possible to use union with an Array. 

    Example:
    ```
    record IdlTestReferences {
      union {null, array<IdlTestReference>} references;
    }
    record IdlTestReference{
      string type;
      union{string, null} optionalId;
    }
    ```

Fixed reference properties validation (e.g. allow 'union {null, array<Something>} someArray').

## [3.4.0] - 12.01.2022

### Added

- Added support for querying and resending failed transactional outbox messages.

## [3.3.0] - 07.01.2022

### Added

- Added metrics and tracing to the transactional outbox.

## [3.2.0] - 22.12.2021

### Changed

- Updated internal parent to 2.2.0 (spring boot 2.6.2)

### Added

- Added transactional outbox.
- Added idempotent message handler annotation and idempotent processing repository.

## [3.1.3] - 26.10.2021

### Changed

- Message Type Registry Validator
  - Message reference values may be of any valid avro primitive type
  - Message references fields can be arrays
  - Command type schema is validated

## [3.1.2] - 06.10.2021

### Fixed

- Fixing ErrorServiceSender not logging trace ids.

## [3.1.1] - 28.09.2021

### Fixed

- Fixing ConsumerLoggingInterceptor not logging trace ids.

## [3.1.0] - 08.07.2021

### Changed

- Simplified spring-kafka configuration to use spring boot defaults wherever possible
- This also allows to use standard spring.kafka.* properties to configure the kafka client and spring-kafka

## [3.0.2] - 22.06.2021

### Changed

- Upgraded to jeap-internal-spring-boot-parent 2.0.2 (spring boot 2.5.1)

## [3.0.1] - 11.06.2021

### Changed

- Validate if the message type from the descriptor is found in a schema to detect typos

## [3.0.0] - 01.06.2021

### Changed

- Update to Spring Boot 2.5.0, including Spring Kafka 2.7.1

## [2.3.0] - 25.05.2021

### Changed

- If autoRegisterSchema is enabled, message senders now register new schemas for message types with the compatibility
  mode defined in the message type registry.

## [2.2.1] - 19.05.2021

### Fixed

- Accept definingSystem as valid attribute in event descriptor validator

## [2.2.0] - 06.05.2021

### Changed

- Deprecate EmbeddedKafkaExtension in favor of @EmbeddedKafka

## [2.1.4] - 03.05.2021

### Fixed

- Allow payload to be an optional field

## [2.1.3] - 28.04.2021

### Changes

- Using default values in MessageProcessingFailedEventBuilder if the original exception is invalid

## [2.1.2] - 23.02.2021

### Changes

- Explicitly delete generated jeap-messaging classes to avoid accidentally deleting other generated files

## [2.1.1] - 25.01.2021

### Changes

- Reduce log level if a component does not contain any contracts

## [2.1.0] - 25.01.2021

### Changes

- Replace KafkaLocalProperties with DefaultKafkaProperties, providing sensible defaults for productive use, with the
  ability to override properties for local usage. Note: Requires to set the following properties also for local
  development under 'jeap.messaging.kafka' (default values for a local docker setup shown in parentheses):
  - bootstrap-servers (http://localhost:9092)
  - schema-registry-url (http://localhost:7781)
  - security-protocol (SASL_PLAINTEXT)
  - set-compatibility-mode-none (true or false, depending on whether you wnat the local schema registry to validate
    schema changes)

### Fixes

- Fix schema validation for minor version updates for commands

## [2.0.0] - 19.01.2021

### Changes

- Propertyname: jeap.event.kafka --> jeap.messaging.kafka

## [1.2.0] - 17.12.2020

- Added prometheus metrics for contract settings

## [1.1.1] - 08.12.2020

- Fixed problems with producing error messages

## [1.1.0] - 03.12.2020

- Renamed jeap-messaging-registry-verifier into jeap-messaging-registry-maven-plugin

## [1.0.1] - 03.12.2020

- Fixed Tests due change on dazit-message-type-registry

## [1.0.0] - 27.11.2020

Initial release, based on jeap-domainevent 7.3.1
