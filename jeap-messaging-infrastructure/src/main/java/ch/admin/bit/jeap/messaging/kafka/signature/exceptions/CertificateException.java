package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

public class CertificateException extends RuntimeException {

    private CertificateException(String message) {
        super(message);
    }

    private CertificateException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CertificateException parsingCertificateFailed(Exception exception) {
        String message = "Could not parse CN from distinguishedName";
        return new CertificateException(message, exception);
    }

    public static CertificateException creatingCertificateFailed(Exception exception) {
        String message = "Could not create certificate";
        return new CertificateException(message, exception);
    }

    public static CertificateException certificateCnNotValid(String applicationName, String subjectDistinguishedName) {
        String message = "CN of certificate " + subjectDistinguishedName + "does not match application name " + applicationName;
        return new CertificateException(message);
    }
}
