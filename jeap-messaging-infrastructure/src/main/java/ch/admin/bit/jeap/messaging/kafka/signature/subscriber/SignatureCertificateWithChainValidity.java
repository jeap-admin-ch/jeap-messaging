package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;


import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;

public record SignatureCertificateWithChainValidity(SignatureCertificate certificate,
                                                    String commonName,
                                                    boolean isChainValid) {

    public boolean isExpired() {
        return certificate().isExpired();
    }
}
