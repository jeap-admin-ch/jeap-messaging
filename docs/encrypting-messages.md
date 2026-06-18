# Encrypting messages

Since 4.9.0 jEAP Messaging can encrypt messages using the jEAP-Crypto library.
`jeap-messaging-infrastructure-kafka` automatically configures encryption in the Kafka
serializer/deserializer if a `KeyIdCryptoService` Spring bean is available — for example provided by
the `jeap-crypto-vault-starter` together with the `jeap-spring-boot-vault-starter`. The wrapping keys
declared in the jEAP Vault crypto configuration are then used to encrypt and decrypt messages.

jEAP Messaging is KMS-agnostic: anything jeap-crypto supports works.

## Declare the wrapping key

The wrapping keys are declared in the jEAP Vault crypto configuration under `jeap.crypto.vault`,
each under a key id (here `messagingKey`):

```yaml
jeap:
  crypto:
    vault:
      default-secret-engine-path: transit
      keys:
        messagingKey:
          key-name: messaging-key
```

## Encrypt a message type

To send a message type encrypted, reference the wrapping key by its key id in the **producer**
contract via the `encryptionKeyId` field on `@JeapMessageProducerContract`:

```java
@JeapMessageProducerContract(value = SomeEvent.TypeRef.class, encryptionKeyId = "messagingKey")
```

Only one message contract per event can exist per encryption key, so projects sometimes adjust
`topic`/`appName` per Maven profile.

## Disable encryption in tests

`jeap.messaging.kafka.messageTypeEncryptionDisabled=true` lets tests run without a jEAP-Crypto
instance. It is forbidden on the acceptance and production environments (see
[Configuration reference](configuration.md)).

```yaml
jeap:
  messaging:
    kafka:
      messageTypeEncryptionDisabled: true
```

## Related

- [jeap-messaging](../README.md)
- [Message contracts](message-contracts.md)
- [Signing messages](signing-messages.md)
- [Configuration reference](configuration.md)
