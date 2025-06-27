package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.SigningTestHelper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SubscriberCertificatesContainerTest {

    @Test
    void getCertificateWithSerialNumber() {
        CertificateServiceEntry certificateServiceEntry1 = new CertificateServiceEntry("jme-example-service", List.of("jme-example-service.crt", "intermediateCA.crt", "rootCA.crt"));
        CertificateServiceEntry certificateServiceEntry2 = new CertificateServiceEntry("jme-messaging-receiverpublisher-service",
                new CertificateChainEntry("chain1", List.of("jme-messaging-receiverpublisher-service.crt", "intermediateCA.crt", "rootCA.crt")),
                new CertificateChainEntry("chain2", List.of("jme-messaging-receiverpublisher-service2.crt", "intermediateCA.crt", "rootCA.crt"))
        );
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry1, certificateServiceEntry2);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        String serialNumber1 = "29 03 FA B4 46 47 D5 8C 49 C5 92 2A D6 75 41 2D 08 A2 D3 32"; // got this from the certificate
        String serialNumber2 = "5C 7C F3 17 F5 E4 CF 00 12 08 50 B6 0A 94 C0 59 4D C9 64 E3"; // got this from the certificate
        String serialNumber3 = "1D 2C 98 E6 CA 78 5C C3 A3 92 2D E7 7F 98 9D 9D 4E A6 96 4D"; // got this from the certificate
        assertNotNull(subscriberCertificatesContainer.getCertificateWithSerialNumber(HexFormat.ofDelimiter(" ").parseHex(serialNumber1)));
        assertNotNull(subscriberCertificatesContainer.getCertificateWithSerialNumber(HexFormat.ofDelimiter(" ").parseHex(serialNumber2)));
        assertNotNull(subscriberCertificatesContainer.getCertificateWithSerialNumber(HexFormat.ofDelimiter(" ").parseHex(serialNumber3)));
        assertNull(subscriberCertificatesContainer.getCertificateWithSerialNumber(HexFormat.ofDelimiter(" ").parseHex("06 BB")));
    }

    @Test
    void certificateChainsValid_valid_whenRightOrderAndValid() {
        List<String> certificateChainFiles = List.of("jme-example-service.crt", "intermediateCA.crt", "rootCA.crt");
        final String serviceName = "jme-example-service";
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(serviceName, certificateChainFiles);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertTrue(subscriberCertificatesContainer.areCertificateChainsValid(serviceName));
    }

    @Test
    void certificateChainsValid_valid_whenRightOrderAndValidSeveralServices() {
        CertificateServiceEntry certificateServiceEntry1 = new CertificateServiceEntry("jme-example-service", List.of("jme-example-service.crt", "intermediateCA.crt", "rootCA.crt"));
        CertificateServiceEntry certificateServiceEntry2 = new CertificateServiceEntry("jme-messaging-receiverpublisher-service", List.of("jme-messaging-receiverpublisher-service.crt", "intermediateCA.crt", "rootCA.crt"));
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry1, certificateServiceEntry2);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertTrue(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry1.serviceName()));
        assertTrue(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry2.serviceName()));
    }

    @Test
    void certificateChainsValid_valid_whenRightOrderAndValidSeveralChains() {
        CertificateServiceEntry certificateServiceEntry = new CertificateServiceEntry("jme-messaging-receiverpublisher-service",
                new CertificateChainEntry("chain1", List.of("jme-messaging-receiverpublisher-service.crt", "intermediateCA.crt", "rootCA.crt")),
                new CertificateChainEntry("chain2", List.of("jme-messaging-receiverpublisher-service2.crt", "intermediateCA.crt", "rootCA.crt"))
        );
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertTrue(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry.serviceName()));
    }


    @Test
    void certificateChainsValid_notValid_whenWrongOrderAndValid() {
        List<String> certificateChainFiles = List.of("rootCA.crt", "intermediateCA.crt", "jme-example-service.crt");
        final String serviceName = "jme-example-service";
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(serviceName, certificateChainFiles);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(serviceName));
    }

    @Test
    void certificateChainsValid_notValid_whenWrongOrderAndValidSeveralServices() {
        CertificateServiceEntry certificateServiceEntry1 = new CertificateServiceEntry("jme-example-service", List.of("jme-example-service.crt", "intermediateCA.crt", "rootCA.crt"));
        CertificateServiceEntry certificateServiceEntry2 = new CertificateServiceEntry("jme-messaging-receiverpublisher-service", List.of("rootCA.crt", "intermediateCA.crt", "jme-messaging-receiverpublisher-service.crt"));
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry1, certificateServiceEntry2);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertTrue(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry1.serviceName()));
        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry2.serviceName()));
    }

    @Test
    void certificateChainsValid_notValid_whenWrongOrderAndValidSeveralChains() {
        CertificateServiceEntry certificateServiceEntry = new CertificateServiceEntry("jme-messaging-receiverpublisher-service",
                new CertificateChainEntry("chain1", List.of("jme-messaging-receiverpublisher-service.crt", "intermediateCA.crt", "rootCA.crt")),
                new CertificateChainEntry("chain2", List.of("rootCA.crt", "intermediateCA.crt", "jme-messaging-receiverpublisher-service2.crt"))
        );
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry.serviceName()));
    }

    @Test
    void certificateChainsValid_notValid_whenNotFullChain() {
        List<String> certificateChainFiles = List.of("jme-example-service.crt", "intermediateCA.crt");
        final String serviceName = "jme-example-service";
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(serviceName, certificateChainFiles);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(serviceName));
    }

    @Test
    void certificateChainsValid_notValid_whenNotFullChainSeveralServices() {
        CertificateServiceEntry certificateServiceEntry1 = new CertificateServiceEntry("jme-example-service", List.of("jme-example-service.crt", "intermediateCA.crt", "rootCA.crt"));
        CertificateServiceEntry certificateServiceEntry2 = new CertificateServiceEntry("jme-messaging-receiverpublisher-service", List.of("jme-messaging-receiverpublisher-service.crt", "intermediateCA.crt"));
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry1, certificateServiceEntry2);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertTrue(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry1.serviceName()));
        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry2.serviceName()));
    }

    @Test
    void certificateChainsValid_notValid_whenNotFullChainSeveralChains() {
        CertificateServiceEntry certificateServiceEntry = new CertificateServiceEntry("jme-messaging-receiverpublisher-service",
                new CertificateChainEntry("chain1", List.of("jme-messaging-receiverpublisher-service.crt", "intermediateCA.crt", "rootCA.crt")),
                new CertificateChainEntry("chain2", List.of("jme-messaging-receiverpublisher-service2.crt", "intermediateCA.crt"))
        );
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(certificateServiceEntry);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(certificateServiceEntry.serviceName()));
    }

    @Test
    void initWithEmptyProps() {
        SignatureSubscriberProperties signatureSubscriberProperties = new SignatureSubscriberProperties(false, null, null, null, null);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);

        assertDoesNotThrow(() -> subscriberCertificatesContainer.init());
    }

    @Test
    void certificateChainsValid_notValid_whenCertificateExpired() {
        List<String> certificateChainFiles = List.of("jme-test-expired-service.crt", "intermediateCA.crt", "rootCA.crt");
        final String serviceName = "jme-test-expired-service";
        SignatureSubscriberProperties signatureSubscriberProperties = prepareProperties(serviceName, certificateChainFiles);
        SubscriberCertificatesContainer subscriberCertificatesContainer = new SubscriberCertificatesContainer(signatureSubscriberProperties);
        subscriberCertificatesContainer.init();

        assertFalse(subscriberCertificatesContainer.areCertificateChainsValid(serviceName));
    }

    private static SignatureSubscriberProperties prepareProperties(String serviceName, List<String> certificateChainFiles) {
        return prepareProperties(new CertificateServiceEntry(serviceName, new CertificateChainEntry("chain", certificateChainFiles)));
    }

    private static SignatureSubscriberProperties prepareProperties(CertificateServiceEntry... certificateEntries) {
        Map<String, Map<String, List<String>>> certificateChains = new HashMap<>();
        for (CertificateServiceEntry certificateServiceEntry : certificateEntries) {
            Map<String, List<String>> certificateChainsByName = new HashMap<>();
            for (CertificateChainEntry certificateChainEntry : certificateServiceEntry.certificateChainEntries()) {
                List<String> certificateChainStrings = certificateChainEntry.certificateChainFiles().stream()
                        .map(certificateChainFile -> "classpath:signing/unittest/" + certificateChainFile)
                        .map(SigningTestHelper::readBytesFromFile)
                        .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                        .collect(Collectors.toList());
                certificateChainsByName.put(certificateChainEntry.chainName(), certificateChainStrings);
            }
            certificateChains.put(certificateServiceEntry.serviceName(), certificateChainsByName);

        }
        return new SignatureSubscriberProperties(false, null, null, null, certificateChains);
    }

    private record CertificateServiceEntry(String serviceName, CertificateChainEntry... certificateChainEntries) {
        public CertificateServiceEntry(String theServiceName, List<String> certificateChainFiles) {
            this(theServiceName, new CertificateChainEntry("chain", certificateChainFiles));
        }

    }

    private record CertificateChainEntry(String chainName, List<String> certificateChainFiles) {
    }
}
