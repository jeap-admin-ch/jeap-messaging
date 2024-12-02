# jEAP Messaging

jEAP Messaging is a library to handle messaging using kafka. It provides

 * A structured definition of message types using Avro Schema and message type descriptors
 * Avro serialization of message types to kafka records
 * Pre-configured Spring Kafka beans for producers and consumers
 * Transactional outbox pattern for reliable message delivery
 * Declaration of consumed/produced message types on topics using Java annotations
 * Error handling for messages that fail during processing (integration with a jeap-errorhandling-service)

## Changes

This library is versioned using [Semantic Versioning](http://semver.org/) and all changes are documented in
[CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/).

## Note

This repository is part the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
