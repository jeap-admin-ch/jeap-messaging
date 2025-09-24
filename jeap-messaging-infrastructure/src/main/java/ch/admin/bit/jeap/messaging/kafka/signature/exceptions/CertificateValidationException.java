package ch.admin.bit.jeap.messaging.kafka.signature.exceptions;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CertificateHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.subscriber.SignatureCertificateWithChainValidity;
import jakarta.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CertificateValidationException extends RuntimeException implements MessageHandlerExceptionInformation {

    private static final String ERROR_CODE = "MESSAGE_CERTIFICATE_VALIDATION_ERROR";
    private static final Temporality DEFAULT_TEMPORALITY = Temporality.TEMPORARY;

    private final Temporality temporality;

    private CertificateValidationException(String message) {
        this(DEFAULT_TEMPORALITY, message);
    }

    private CertificateValidationException(final Temporality temporality, String message) {
        super(message);
        this.temporality = temporality;
    }

    public static CertificateValidationException certificateNotFound(byte[] certificateSerialNumber) {
        String message = String.format("Could not find certificate with serial number %s", CertificateHelper.serialNumberHexString(certificateSerialNumber));
        return new CertificateValidationException(message);
    }

    public static CertificateValidationException certificateExpired(SignatureCertificateWithChainValidity certificate) {
        String message = String.format("Certificate %s has expired", certificate);
        return new CertificateValidationException(Temporality.PERMANENT, message);
    }

    public static CertificateValidationException invalidChain(SignatureCertificateWithChainValidity certificate) {
        String message = String.format("Certificate %s has an invalid certificate chain", certificate);
        return new CertificateValidationException(message);
    }

    public static CertificateValidationException certificateCommonNameNotValid(String serviceName, String commonName) {
        String message = String.format("Certificate common name %s is different to service name %s", commonName, serviceName);
        return new CertificateValidationException(Temporality.PERMANENT, message);
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }


    @Override
    public Temporality getTemporality() {
        return temporality;
    }

    @Nullable
    @Override
    public String getStackTraceAsString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        this.printStackTrace(pw);
        return sw.toString();
    }
}
