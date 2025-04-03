package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;


import ch.admin.bit.jeap.messaging.kafka.signature.common.CertificateHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RefreshScope
@RequiredArgsConstructor
@Slf4j
public class SubscriberCertificatesContainer {

    private final SignatureSubscriberProperties signatureSubscriberProperties;
    private Map<String, CertificateChainEntry> certificateChainsByServiceName;

    @PostConstruct
    public void init() {
        this.certificateChainsByServiceName = toRealCertificatesChain(signatureSubscriberProperties.certificateChains());
        validateCertificates(certificateChainsByServiceName);
        List<String> certificateSerialNumbers = new ArrayList<>();
        for (CertificateChainEntry certificateChainEntry : certificateChainsByServiceName.values()) {
            for (CertificateChain certificateChain : certificateChainEntry.chains()) {
                certificateSerialNumbers.add(CertificateHelper.serialNumberHexString(certificateChain.getLeafCertificate().getSerialNumber()));
            }
        }
        log.info("SubscriberCertificatesContainer initialized, certificate serial numbers: {}", certificateSerialNumbers);
    }

    public SignatureCertificateWithChainValidity getCertificateWithSerialNumber(byte[] certificateSerialNumber) {
        for (CertificateChainEntry certificateChainEntry : certificateChainsByServiceName.values()) {
            SignatureCertificateWithChainValidity certificateWithSerialNumber = certificateChainEntry.getCertificateWithSerialNumber(certificateSerialNumber);
            if (certificateWithSerialNumber != null) {
                return certificateWithSerialNumber;
            }
        }
        return null;
    }

    /**
     * 4 testing purposes
     */
    boolean areCertificateChainsValid(String serviceName) {
        return certificateChainsByServiceName.get(serviceName).chains().stream().allMatch(CertificateChain::isValid);
    }

    private void validateCertificates(Map<String, CertificateChainEntry> certificateChainsByServiceName) {
        log.debug("Validating properties");
        validateCertificateChain(certificateChainsByServiceName);
        log.debug("Validating properties done");
    }


    private void validateCertificateChain(Map<String, CertificateChainEntry> certificateChainsByServiceName) {
        log.debug("Validating certificate chain");
        for (CertificateChainEntry certificateChainEntry : certificateChainsByServiceName.values()) {
            validateCertificateChain(certificateChainEntry);
        }
        log.debug("Validating certificate chain done");
    }

    private void validateCertificateChain(CertificateChainEntry certificateChainEntry) {
        log.debug("Validating certificate chain for service {}", certificateChainEntry.service());
        List<CertificateChain> chains = certificateChainEntry.chains();
        for (int i = 0; i < chains.size(); i++) {
            CertificateChain certificateChain = chains.get(i);
            List<SignatureCertificate> certificates = certificateChain.getCertificates();
            log.debug("Validating certificate chain {} (index {}) for service {} and certificates {}", certificateChain.getName(), i, certificateChainEntry.service(),
                    certificates.stream().map(SignatureCertificate::getSubjectDistinguishedName).collect(Collectors.joining(" -> ")));
            List<X509Certificate> x509Certificates = certificates.stream()
                    .map(SignatureCertificate::certificate)
                    .collect(Collectors.toList());
            try {
                CertificateHelper.validateCertificateChain(x509Certificates);
                certificateChain.updateValidity(true);
            } catch (CertificateException e) {
                log.error(String.format("Certificate chain validation failed for service %s and chain %s", certificateChainEntry.service(), certificateChain.getName()), e);
                certificateChain.updateValidity(false);
            }
        }
    }

    private Map<String, CertificateChainEntry> toRealCertificatesChain(Map<String, Map<String, List<String>>> certificateChains) {
        Map<String, CertificateChainEntry> result = new HashMap<>();
        for (Map.Entry<String, Map<String, List<String>>> certificateChainByName : certificateChains.entrySet()) {
            String serviceName = certificateChainByName.getKey();
            List<CertificateChain> convertedCertificateChainsOfService = getConvertedCertificateChain(certificateChainByName, serviceName);
            result.put(serviceName, new CertificateChainEntry(serviceName, convertedCertificateChainsOfService));
        }
        return result;
    }

    private List<CertificateChain> getConvertedCertificateChain(Map.Entry<String, Map<String, List<String>>> certificateChainByName, String serviceName) {
        List<CertificateChain> realCertificateChainsOfService = new ArrayList<>();
        Map<String, List<String>> certificateChainsOfService = certificateChainByName.getValue();
        for (Map.Entry<String, List<String>> certificateChainOfService : certificateChainsOfService.entrySet()) {
            String chainName = certificateChainOfService.getKey();
            log.debug("Adding certificate chain {} for service {}", chainName, serviceName);
            List<SignatureCertificate> convertedCertificateChain = certificateChainOfService.getValue().stream()
                    .map(certificateString -> SignatureCertificate.fromBytes(certificateString.getBytes(StandardCharsets.UTF_8)))
                    .collect(Collectors.toList());
            CertificateChain certificateChain = new CertificateChain(chainName, convertedCertificateChain);
            realCertificateChainsOfService.add(certificateChain);
        }
        return realCertificateChainsOfService;
    }

    record CertificateChainEntry(String service, List<CertificateChain> chains) {

        SignatureCertificateWithChainValidity getCertificateWithSerialNumber(byte[] certificateSerialNumber) {
            return chains.stream()
                    .map(certificateChain -> certificateChain.getCertificateWithSerialNumber(certificateSerialNumber))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
    }

    @RequiredArgsConstructor
    @Getter
    private class CertificateChain {

        private final String name;
        private final List<SignatureCertificate> certificates; //  a list of the certificates, first certificate in the list must be the leaf, last the root
        private boolean valid = false;

        SignatureCertificateWithChainValidity getCertificateWithSerialNumber(byte[] certificateSerialNumber) {
            SignatureCertificate leafCertificate = getLeafCertificate();
            if (Objects.deepEquals(leafCertificate.getSerialNumber(), certificateSerialNumber)) {
                return new SignatureCertificateWithChainValidity(leafCertificate, valid);
            }
            return null;
        }

        SignatureCertificate getLeafCertificate() {
            return certificates.getFirst();
        }


        public void updateValidity(boolean valid) {
            this.valid = valid;
        }
    }

}
