package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;


import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;

public record SignatureCertificateWithChainValidity(SignatureCertificate certificate, boolean isChainValid) {

    public String getSubjectDistinguishedName() {
        return certificate().getSubjectDistinguishedName();
    }

    public boolean isExpired() {
        return certificate().isExpired();
    }
}
