package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.avro.errorevent.MessageHandlerExceptionInformation;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureMetricsService;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateValidationException;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureValidationException;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.SignatureAuthenticityMessageException;
import ch.admin.bit.jeap.messaging.model.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DefaultSignatureAuthenticityService implements SignatureAuthenticityService {

    private final SubscriberValidationPropertiesContainer validationPropertiesContainer;
    private final CertificateAndSignatureVerifier certificateAndSignatureVerifier;
    private final SignatureMetricsService signatureMetricsService;
    private final SubscriberCertificatesContainer subscriberCertificatesContainer;
    private AtomicLong signatureRequiredState;

    public DefaultSignatureAuthenticityService(SubscriberValidationPropertiesContainer validationPropertiesContainer,
                                               CertificateAndSignatureVerifier certificateAndSignatureVerifier,
                                               SubscriberCertificatesContainer subscriberCertificatesContainer,
                                               Optional<SignatureMetricsService> signatureMetricsService) {
        this.validationPropertiesContainer = validationPropertiesContainer;
        this.certificateAndSignatureVerifier = certificateAndSignatureVerifier;
        this.signatureMetricsService = signatureMetricsService.orElse(null);
        this.subscriberCertificatesContainer = subscriberCertificatesContainer;
        log.info("SignatureAuthenticityService initialized, requireSignature (strict mode): {}", validationPropertiesContainer.isSignatureRequired());
    }

    @PostConstruct
    void init() {
        CryptoProviderHelper.installCryptoProvider();
        initMetrics();
    }

    @Override
    public void checkAuthenticityValue(Object deserialized, Headers headers, byte[] bytesToValidate) {
        if (deserialized instanceof Message message) {
            if (headers == null) {
                throw MessageSignatureValidationException.headersMissing(true);
            }
            String messageTypeName = message.getType().getName();
            try {
                doCheckAuthenticityValue(message, headers, bytesToValidate);
                recordSignatureValidation(messageTypeName, true);
            } catch (Exception exception) {
                recordSignatureValidation(messageTypeName, false);
                if (exception instanceof MessageHandlerExceptionInformation messageHandlerExceptionInformation) {
                    throw SignatureAuthenticityMessageException.fromMessageHandlerExceptionInformation(message, messageHandlerExceptionInformation, exception);
                }
                throw exception;
            }
        } else {
            throw MessageSignatureValidationException.notAllowedMessageType(deserialized);
        }
    }

    @Override
    public void checkAuthenticityKey(Headers headers, byte[] bytesToValidate) {
        if (headers == null) {
            throw MessageSignatureValidationException.headersMissing(true);
        }
        Header certificateHeader = headers.lastHeader(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY);
        Header signatureHeader = headers.lastHeader(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY);
        boolean signatureRequired = false; // We can't determine this here
        validateHeaders(signatureRequired, signatureHeader, certificateHeader, "unknown", "unknown");

        if (certificateHeader != null) { // then both headers are set
            SignatureCertificateWithChainValidity cert = getCertificate(certificateHeader.value());
            boolean result = certificateAndSignatureVerifier.verifyKeySignature(bytesToValidate, signatureHeader.value(), cert);
            if (!result) {
                throw MessageSignatureValidationException.invalidSignatureKey();
            }
        }
    }

    private void doCheckAuthenticityValue(Message message, Headers headers, byte[] bytesToValidate) {
        String messageTypeName = message.getType().getName();
        String service = message.getPublisher().getService();

        Header certificateHeader = headers.lastHeader(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY);
        Header signatureHeader = headers.lastHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY);
        boolean signatureRequired = validationPropertiesContainer.isSignatureRequired(messageTypeName);
        validateHeaders(signatureRequired, signatureHeader, certificateHeader, messageTypeName, service);

        if (certificateHeader != null) { // then both headers are set
            SignatureCertificateWithChainValidity cert = getCertificate(certificateHeader.value());
            validateAllowedPublisher(messageTypeName, service, cert);
            boolean result = certificateAndSignatureVerifier.verifyValueSignature(service, bytesToValidate, signatureHeader.value(), cert);
            if (!result) {
                throw MessageSignatureValidationException.invalidSignatureValue(messageTypeName, service);
            }
        }
    }

    private void validateAllowedPublisher(String messageTypeName, String service, SignatureCertificateWithChainValidity cert) {
        boolean publisherAllowedForMessage = validationPropertiesContainer.isPublisherAllowedForMessage(messageTypeName, service);
        if (!publisherAllowedForMessage && !certificateAndSignatureVerifier.isPrivilegedProducer(cert)) {
            throw MessageSignatureValidationException.publisherNotAllowed(messageTypeName, service);
        }
    }

    private SignatureCertificateWithChainValidity getCertificate(byte[] certificateSerialNumber) {
        SignatureCertificateWithChainValidity certificateWithChainValidity = subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber);
        if (certificateWithChainValidity == null) {
            throw CertificateValidationException.certificateNotFound(certificateSerialNumber);
        }
        return certificateWithChainValidity;
    }

    private void validateHeaders(boolean signatureRequired, Header signatureHeader, Header certificateHeader, String messageTypeName, String service) {
        if (signatureRequired) {
            if (signatureHeader == null || certificateHeader == null) {
                throw MessageSignatureValidationException.strictModeSignatureHeadersMissing(messageTypeName, service);
            }
        } else if (signatureHeader == null && certificateHeader != null) {
            throw MessageSignatureValidationException.signatureHeaderMissing(messageTypeName, service);
        } else if (signatureHeader != null && certificateHeader == null) {
            throw MessageSignatureValidationException.certificateHeaderMissing(messageTypeName, service);
        }
    }

    private void recordSignatureValidation(String keyMessageTypeMetricName, boolean result) {
        if (signatureMetricsService != null) {
            signatureMetricsService.recordSignatureValidation(keyMessageTypeMetricName, result);
        }
    }

    private void initMetrics() {
        signatureRequiredState = new AtomicLong(validationPropertiesContainer.isSignatureRequired() ? 1 : 0);
        if (signatureMetricsService != null) {
            signatureMetricsService.initSignatureRequiredMetricName(() -> signatureRequiredState.get());
        }
    }
}
