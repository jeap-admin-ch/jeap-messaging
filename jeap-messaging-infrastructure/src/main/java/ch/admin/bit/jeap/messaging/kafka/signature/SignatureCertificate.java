package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@RequiredArgsConstructor
public class SignatureCertificate {

    @Getter
    private final String subjectDistinguishedName;
    @Getter
    private final byte[] serialNumber;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;

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
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(certificateBytes)
            );
            String subjectDistinguishedName = certificate.getSubjectX500Principal().getName();

            return new SignatureCertificate(subjectDistinguishedName, certificate.getSerialNumber().toByteArray(),
                    convertToLocalDate(certificate.getNotBefore()), convertToLocalDate(certificate.getNotAfter()));
        } catch (java.security.cert.CertificateException exception) {
            throw CertificateException.creatingCertificateFailed(exception);
        }
    }

    private static LocalDateTime convertToLocalDate(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}