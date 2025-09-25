package ch.admin.bit.jeap.messaging.kafka.signature.benchmark;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventIdentity;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventType;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import ch.admin.bit.jeap.messaging.kafka.signature.SigningTestHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.DefaultSignatureService;
import ch.admin.bit.jeap.messaging.kafka.signature.publisher.SignaturePublisherProperties;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.*;
import ch.admin.bit.jme.declaration.DeclarationPayload;
import ch.admin.bit.jme.declaration.DeclarationReferences;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import com.amazon.corretto.crypto.provider.AmazonCorrettoCryptoProvider;
import lombok.SneakyThrows;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.scheduling.support.NoOpTaskScheduler;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Utility class providing methods for signature benchmarking.
 */
class Benchmarks {
    private static final String TEST_SERVICE_NAME = "jme-messaging-receiverpublisher-service";
    private static final String CERT_PATH = "/perftest/perftest.crt";
    private static final String INTERMEDIATE_CA_PATH = "/perftest/intermediateCA.crt";
    private static final String ROOT_CA_PATH = "/perftest/rootCA.crt";

    /**
     * Signs the provided serialized bytes using the signature service
     * and returns the resulting signature.
     */
    public static byte[] sign(byte[] serializedBytes) {
        SignatureService signatureService = createSignatureService();
        RecordHeaders headers = new RecordHeaders();
        signatureService.injectSignature(headers, serializedBytes, false);
        return headers.lastHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY).value();
    }

    /**
     * Creates a properly configured SignatureAuthenticityService for the benchmark
     */
    @SneakyThrows
    static SignatureAuthenticityService createAuthenticityService() {
        CryptoProviderHelper.installCryptoProvider();
        
        // Load certificates
        Map<String, Map<String, List<String>>> certificateChains = createCertificateChains();
        
        // Create and configure subscriber properties
        boolean requireSignature = true;
        SignatureSubscriberProperties props = new SignatureSubscriberProperties(
                requireSignature, Set.of(), Set.of(), Map.of(), certificateChains, false);
        
        // Initialize containers and validators
        SubscriberValidationPropertiesContainer validationContainer = initializeValidationContainer(props);
        SubscriberCertificatesContainer certificatesContainer = initializeCertificatesContainer(props);
        CertificateAndSignatureVerifier verifier = createVerifier(certificatesContainer);

        return new DefaultSignatureAuthenticityService(validationContainer, verifier, certificatesContainer, Optional.empty());
    }

    /**
     * Creates a properly configured SignatureService for the benchmark
     */
    @SneakyThrows
    static SignatureService createSignatureService() {
        CryptoProviderHelper.installCryptoProvider();
        if (!CryptoProviderHelper.getSignatureInstance().getProvider().getName().equals(AmazonCorrettoCryptoProvider.PROVIDER_NAME)) {
            throw new IllegalStateException("AmazonCorrettoCryptoProvider not installed");
        }
        
        // Load key and certificate
        String key = SigningTestHelper.MESSAGE_RECEIVER_PRIVATE_KEY;
        String cert = load(CERT_PATH);
        
        SignaturePublisherProperties props = new SignaturePublisherProperties(key, cert);
        
        // Create and initialize service
        DefaultSignatureService service = new DefaultSignatureService(
                props,
                new NoOpTaskScheduler(), 
                Optional.empty(), 
                TEST_SERVICE_NAME
        );
        service.init();
        return service;
    }

    static JmeDeclarationCreatedEvent createJmeDeclarationCreatedEvent() {
        return JmeDeclarationCreatedEvent.newBuilder()
                .setTypeBuilder(AvroDomainEventType.newBuilder()
                        .setName("JmeDeclarationCreatedEvent")
                        .setVersion("1.4.0"))
                .setIdentityBuilder(AvroDomainEventIdentity.newBuilder()
                        .setCreated(Instant.now())
                        .setEventId("1234")
                        .setIdempotenceId("4567"))
                .setPublisherBuilder(AvroDomainEventPublisher.newBuilder()
                        .setService(TEST_SERVICE_NAME)
                        .setSystem("JME"))
                .setReferencesBuilder(DeclarationReferences.newBuilder())
                .setProcessId("processId-1234")
                .setPayloadBuilder(DeclarationPayload.newBuilder()
                        .setMessage("message the quick brown fox jumps over the lazy dog 012345678912 message"))
                .build();
    }

    @SneakyThrows
    static byte[] serializeJmeDeclarationCreatedEvent() {
        JmeDeclarationCreatedEvent event = createJmeDeclarationCreatedEvent();
        // While this is not a fully correct serialization as it is missing the schema registry specific prefixes,
        // it is good enough for the benchmark
        return event.toByteBuffer().array();
    }

    /**
     * Loads content from a resource file, filtering out comment lines.
     */
    private static String load(String name) throws IOException {
        @SuppressWarnings({"resource", "DataFlowIssue"})
        String content = new String(SignatureServiceBenchmark.class.getResourceAsStream(name).readAllBytes());
        return content.lines()
                .filter(line -> !line.startsWith("#"))
                .collect(joining("\n"));
    }

    /**
     * Creates certificate chains map for testing.
     */
    private static Map<String, Map<String, List<String>>> createCertificateChains() throws IOException {
        String cert = load(CERT_PATH);
        String intermediateCa = load(INTERMEDIATE_CA_PATH);
        String rootCa = load(ROOT_CA_PATH);
        
        Map<String, List<String>> serviceChains = Map.of("testchain", List.of(cert, intermediateCa, rootCa));
        
        return Map.of(TEST_SERVICE_NAME, serviceChains);
    }

    private static SubscriberValidationPropertiesContainer initializeValidationContainer(
            SignatureSubscriberProperties props) {
        SubscriberValidationPropertiesContainer container = new SubscriberValidationPropertiesContainer(props);
        container.init();
        return container;
    }

    private static SubscriberCertificatesContainer initializeCertificatesContainer(
            SignatureSubscriberProperties props) {
        SubscriberCertificatesContainer container = new SubscriberCertificatesContainer(props);
        container.init();
        return container;
    }

    private static CertificateAndSignatureVerifier createVerifier(
            SubscriberCertificatesContainer certificatesContainer) {
        SignatureCertificateValidator certificateValidator = new SignatureCertificateValidator();
        SignatureVerifier signatureVerifier = new SignatureVerifier();
        SignatureSubscriberProperties props = new SignatureSubscriberProperties(true, null,
                Set.of(), null, null, false);
        return new CertificateAndSignatureVerifier(certificateValidator, signatureVerifier, props);
    }

    static byte[] simulateLargeEvent() {
        // this event is 200 bytes long
        byte[] serializedBytes = serializeJmeDeclarationCreatedEvent();
        // create a large event by repeating the event 1000 times (around 200KB)
        byte[] largeEvent = new byte[serializedBytes.length * 1000];
        for (int i = 0; i < 1000; i++) {
            System.arraycopy(serializedBytes, 0, largeEvent, i * serializedBytes.length, serializedBytes.length);
        }
        return largeEvent;
    }

    static void assertValueSigned(Headers headers) {
        if (headers.lastHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY) == null) {
            throw new IllegalStateException("Headers do not contain value signature");
        }
    }

    static void assertKeyAndValueSigned(Headers headers) {
        if (headers.lastHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY) == null ||
                headers.lastHeader(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY) == null) {
            throw new IllegalStateException("Headers do not contain both key and value signatures");
        }
    }
}
