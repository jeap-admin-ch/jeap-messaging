# Enabling message signing

Steps to turn on message signing so subscribers can establish which service actually published a
message. See [Signing messages](../signing-messages.md) for the flow, full property reference, metrics
and failure cases.

1. Get a private key and matching certificate for your service, with a CN equal to `spring.application.name`
   (which `jeap.messaging.kafka.serviceName` — the publisher name in the message identity — defaults to,
   unless overridden separately).
   - **Production:** obtained the same way as your service's other secrets, through your platform's
     PKI / secrets management — this is outside jEAP Messaging's scope, which only consumes the PEM
     values via configuration.
   - **Local/dev/test:** a self-signed certificate is enough. Include `-nodes` so the key is written
     unencrypted (`BEGIN PRIVATE KEY`) — `signature-key` only accepts an unencrypted PKCS#8 key, and an
     encrypted one (`BEGIN ENCRYPTED PRIVATE KEY`, which is what you get without `-nodes`) fails to
     even base64-decode:
     ```shell
     openssl req -x509 -nodes -newkey rsa:2048 -keyout service-key.pem -out service-cert.pem -days 365 -subj "/CN=my-service"
     ```
2. Configure the publisher side with that key and certificate
   (`jeap.messaging.authentication.publisher.*`):

   ```yaml
   jeap:
     messaging:
       authentication:
         publisher:
           signature-key: |
             -----BEGIN PRIVATE KEY-----
             ...
             -----END PRIVATE KEY-----
           signature-certificate: |
             -----BEGIN CERTIFICATE-----
             ...
             -----END CERTIFICATE-----
   ```

   At startup jEAP checks that the key matches the certificate and that the certificate's CN matches
   `spring.application.name` — the service fails to start otherwise. That check is against the static
   Spring property, not the message's `publisher.service` field itself; the two only coincide because
   `jeap.messaging.kafka.serviceName` defaults to `spring.application.name` by convention. The
   per-message publisher identity is what subscribers actually verify against, on each received
   message.
3. Distribute the certificate (chain) to every subscriber that needs to verify it
   (`jeap.messaging.authentication.subscriber.certificate-chains`). This is keyed by publisher service
   name, then by an arbitrary **chain name** of your choosing (so a publisher can have more than one
   chain active at once, e.g. during rotation — see step 6), and each named chain is a list of
   `[leaf, optional intermediates, root]`:

   ```yaml
   jeap:
     messaging:
       authentication:
         subscriber:
           certificate-chains:
             my-publishing-service:
               chain-2026:
                 - |
                   -----BEGIN CERTIFICATE-----
                   ... leaf ...
                   -----END CERTIFICATE-----
                 - |
                   -----BEGIN CERTIFICATE-----
                   ... root ...
                   -----END CERTIFICATE-----
   ```
4. Roll out without breaking existing consumers, in this order:
   1. Deploy subscribers first, leaving `require-signature` at its default (`false`). With it `false`,
      the subscriber only checks that the `jeap-sign`/`jeap-sign-key`/`jeap-cert` headers are consistently
      present or absent — it does **not** yet cryptographically verify the signature of a signed message,
      so unsigned traffic from not-yet-updated publishers keeps flowing and signed traffic isn't actually
      checked either.
   2. Deploy the publisher with `signature-key`/`signature-certificate` configured. Messages now carry
      signature headers, but with `require-signature` still `false` on the subscribers, these signatures
      are not yet verified — this step only gets the headers flowing so step 3 can enable verification
      without an outage.
   3. Once every relevant publisher signs its messages, set `require-signature: true` on the
      subscribers to actually enable signature verification and enforce it — optionally whitelisting
      message types that intentionally stay unsigned via `accept-unsigned-messagetype-whitelist`. Do
      this deliberately, in a step where you can watch the metrics below, since it's the point at which
      verification (and thus any configuration/certificate problem) first takes effect.
5. Watch the rollout with the signing metrics: `jeap_messaging_signature_validation_outcome`
   (OK/NOK, per app and message type) to confirm verification is succeeding — note this metric is only
   meaningful once `require-signature: true` is set, since before that no value signature is actually
   verified — and `jeap_messaging_signature_certificate_days_remaining` so an expiring certificate
   doesn't surprise you.
6. Before a certificate expires, request/generate its replacement for the same CN, then rotate without a
   gap by adding rather than replacing: add the new certificate chain to the subscribers under a new
   chain name next to the existing one (e.g. `chain-2027` alongside `chain-2026`) and deploy — subscribers
   now accept either certificate — then roll the publisher's `signature-key`/`signature-certificate` to
   the new one. Only remove the old chain name from the subscribers once you're sure no message signed
   with the old key can still turn up — i.e. after both the normal consumer-lag/processing window *and*
   the longest period you could still replay messages from have passed, not just after a successful
   verification with the new key. Subscribers resolve the signing certificate by serial number
   (`SubscriberCertificatesContainer.getCertificateWithSerialNumber()`), so once the old chain is removed,
   any message that is still signed with the old certificate — from a replay or a straggling consumer —
   can no longer be verified; with `require-signature: true` that fails the message to the Error Handling
   Service instead of just missing the OK-metric. Plan the rotation cadence with enough lead time before
   certificate expiry to cover this.

## Related

- [jeap-messaging](../../README.md)
- [Signing messages](../signing-messages.md)
- [Error handling](../error-handling.md)
- [Configuration reference](../configuration.md)
