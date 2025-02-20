package ch.admin.bit.jeap.messaging.kafka.signature.common;

import lombok.RequiredArgsConstructor;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@RequiredArgsConstructor
public class SignatureCertificate {

    private final X509Certificate certificate;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;

    public String getSubjectDistinguishedName() {
        return certificate.getSubjectX500Principal().getName();
    }

    public byte[] getSerialNumber() {
        return certificate.getSerialNumber().toByteArray();
    }
    public PublicKey getPublicKey() {
        return certificate.getPublicKey();
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(validTo);
    }

    public boolean isNotYetValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(validFrom);
    }

    public long getValidityRemainingDays() {
        LocalDateTime now = LocalDateTime.now();
        return ChronoUnit.DAYS.between(now, validTo);
    }

    public static SignatureCertificate fromBytes(byte[] certificateBytes) {
        X509Certificate certificate = CertificateHelper.generateCertificate(certificateBytes);
        return new SignatureCertificate(certificate, convertToLocalDate(certificate.getNotBefore()), convertToLocalDate(certificate.getNotAfter()));
    }

    private static LocalDateTime convertToLocalDate(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Override
    public String toString() {
        return certificate.toString();
    }
}
