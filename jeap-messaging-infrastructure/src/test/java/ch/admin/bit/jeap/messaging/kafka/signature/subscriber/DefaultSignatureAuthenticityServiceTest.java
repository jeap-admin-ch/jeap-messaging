package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureMetricsService;
import ch.admin.bit.jeap.messaging.kafka.signature.common.SignatureCertificate;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.MessageSignatureValidationException;
import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.SignatureAuthenticityMessageException;
import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.messaging.model.MessagePublisher;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultSignatureAuthenticityServiceTest {

    private SubscriberValidationPropertiesContainer validationPropertiesContainer;
    private CertificateAndSignatureVerifier certificateAndSignatureVerifier;
    private Optional<SignatureMetricsService> signatureMetricsService;
    private SubscriberCertificatesContainer subscriberCertificatesContainer;
    private DefaultSignatureAuthenticityService signatureAuthenticityService;
    private SignatureCertificateWithChainValidity certificateWithChainValidity;

    private static final String SERVICE_NAME = "jme-messaging-receiverpublisher-service";

    @BeforeEach
    void setUp() {
        validationPropertiesContainer = mock(SubscriberValidationPropertiesContainer.class);
        certificateAndSignatureVerifier = mock(CertificateAndSignatureVerifier.class);
        signatureMetricsService = Optional.empty();
        subscriberCertificatesContainer = Mockito.mock(SubscriberCertificatesContainer.class);
        certificateWithChainValidity = Mockito.mock(SignatureCertificateWithChainValidity.class);
        SignatureCertificate signatureCertificate = Mockito.mock(SignatureCertificate.class);
        when(certificateWithChainValidity.certificate()).thenReturn(signatureCertificate);
        when(certificateWithChainValidity.commonName()).thenReturn(SERVICE_NAME);

        signatureAuthenticityService = new DefaultSignatureAuthenticityService(validationPropertiesContainer, certificateAndSignatureVerifier,
                subscriberCertificatesContainer, signatureMetricsService);
    }

    @Test
    void checkAuthenticityValue_doNotFail_whenAuthenticityCheckOk() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = {1, 2, 3};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);
        when(certificateAndSignatureVerifier.verifyValueSignature("my-service", bytesToValidate, signatureValue, certificateWithChainValidity)).thenReturn(true);

        signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate);

        verify(validationPropertiesContainer).isSignatureRequired();
        verify(validationPropertiesContainer).isPublisherAllowedForMessage("MyMessage", "my-service");
        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verify(certificateAndSignatureVerifier).verifyValueSignature("my-service", bytesToValidate, signatureValue, certificateWithChainValidity);
    }

    @Test
    void checkAuthenticityValue_doNotFail_whenAuthenticityCheckOk_withMetricsService() {
        SignatureMetricsService signatureMetricsService = mock(SignatureMetricsService.class);
        signatureAuthenticityService = new DefaultSignatureAuthenticityService(validationPropertiesContainer, certificateAndSignatureVerifier, subscriberCertificatesContainer, Optional.of(signatureMetricsService));
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = {1, 2, 3};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);
        when(certificateAndSignatureVerifier.verifyValueSignature("my-service", bytesToValidate, signatureValue, certificateWithChainValidity)).thenReturn(true);

        signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate);

        verify(validationPropertiesContainer, atLeast(1)).isSignatureRequired();
        verify(validationPropertiesContainer).isPublisherAllowedForMessage("MyMessage", "my-service");
        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verify(certificateAndSignatureVerifier).verifyValueSignature("my-service", bytesToValidate, signatureValue, certificateWithChainValidity);
        verify(signatureMetricsService).recordSignatureValidation("MyMessage", true);
    }

    @Test
    void checkAuthenticityValue_doNotFail_whenSignatureNotRequiredSignatureValueNotSetAndCertificateNotSet_Payload() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = null;
        byte[] signatureValue = null;
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(false);

        signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate);

        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityValue_fail_whenPublishingNotAllowedForService() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = {1, 2, 3};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(false);

        assertThrows(SignatureAuthenticityMessageException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired();
        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verify(validationPropertiesContainer).isPublisherAllowedForMessage("MyMessage", "my-service");
        verifyNoMoreInteractions(validationPropertiesContainer);
    }

    @Test
    void checkAuthenticityValue_doNotFail_whenProducerIsAllowedToImpersonate() {
        Message message = createMessage("MyMessage", "my-impersonated-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = {1, 2, 3};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        // Publisher is not allowed for message, but is privileged and thus allowed to impersonate another service
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-impersonated-service")).thenReturn(false);
        when(certificateAndSignatureVerifier.isPrivilegedProducer(certificateWithChainValidity)).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);
        when(certificateAndSignatureVerifier.verifyValueSignature("my-impersonated-service", bytesToValidate, signatureValue, certificateWithChainValidity)).thenReturn(true);

        signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate);

        verify(validationPropertiesContainer).isSignatureRequired();
        verify(validationPropertiesContainer).isPublisherAllowedForMessage("MyMessage", "my-impersonated-service");
        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verify(certificateAndSignatureVerifier).verifyValueSignature("my-impersonated-service", bytesToValidate, signatureValue, certificateWithChainValidity);
    }

    @Test
    void checkAuthenticityValue_fail_whenAuthenticityCheckNotOk() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = {1, 2, 3};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);
        when(certificateAndSignatureVerifier.verifyValueSignature("my-service", bytesToValidate, signatureValue, certificateWithChainValidity)).thenReturn(false);

        assertThrows(SignatureAuthenticityMessageException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));
    }

    @Test
    void checkAuthenticityValue_doFail_whenSignatureRequiredButSignatureNotSet() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = null;
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);

        assertThrows(SignatureAuthenticityMessageException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityValue_doFail_whenSignatureRequiredButCertificateNotSet() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = null;
        byte[] signatureValue = {7, 8, 9};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(true);

        assertThrows(SignatureAuthenticityMessageException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityValue_doFail_whenSignatureNotRequiredCertificateSetButSignatureValueNotSet() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = null;
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(false);

        assertThrows(SignatureAuthenticityMessageException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityValue_doFail_whenSignatureNotRequiredSignatureValueSetButCertificateNotSet() {
        Message message = createMessage("MyMessage", "my-service");
        byte[] certificateSerialNumber = null;
        byte[] signatureValue = {7, 8, 9};
        byte[] signatureKey = {4, 5, 6};

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(validationPropertiesContainer.isPublisherAllowedForMessage("MyMessage", "my-service")).thenReturn(true);
        when(validationPropertiesContainer.isSignatureRequired("MyMessage")).thenReturn(false);

        assertThrows(SignatureAuthenticityMessageException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired("MyMessage");
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityValue_doFail_whenHeadersMissing() {
        Message message = createMessage("MyMessage", "my-service");

        Headers headers = null;
        byte[] bytesToValidate = {1, 1, 1};

        assertThrows(MessageSignatureValidationException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired();
        verifyNoMoreInteractions(validationPropertiesContainer);
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityValue_doFail_whenNotAMessage() {
        Object message = new Object();
        byte[] certificateSerialNumber = {4, 5, 6};
        byte[] signatureValue = {7, 8, 9};
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        assertThrows(MessageSignatureValidationException.class, () -> signatureAuthenticityService.checkAuthenticityValue(message, headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired();
        verifyNoMoreInteractions(validationPropertiesContainer);
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityKey_doNotFail_whenSignatureRequiredAndAuthenticityCheckOk() {
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = null;
        byte[] signatureKey = {4, 5, 6};

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(certificateAndSignatureVerifier.verifyKeySignature(bytesToValidate, signatureKey, certificateWithChainValidity)).thenReturn(true);

        signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate);

        verify(certificateAndSignatureVerifier).verifyKeySignature(bytesToValidate, signatureKey, certificateWithChainValidity);
    }

    @Test
    void checkAuthenticityKey_doNotFail_whenSignatureRequiredAndAuthenticityCheckOk_withMetricsService() {
        SignatureMetricsService signatureMetricsService = mock(SignatureMetricsService.class);
        signatureAuthenticityService = new DefaultSignatureAuthenticityService(validationPropertiesContainer,
                certificateAndSignatureVerifier, subscriberCertificatesContainer, Optional.of(signatureMetricsService));

        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = null;
        byte[] signatureKey = {4, 5, 6};

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(certificateAndSignatureVerifier.verifyKeySignature(bytesToValidate, signatureKey, certificateWithChainValidity)).thenReturn(true);

        signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate);

        verify(certificateAndSignatureVerifier).verifyKeySignature(bytesToValidate, signatureKey, certificateWithChainValidity);
        verifyNoInteractions(signatureMetricsService);
    }

    @Test
    void checkAuthenticityKey_doNotFail_whenSignatureKeyNotSetAndCertificateNotSet_Key() {
        byte[] certificateSerialNumber = null;
        byte[] signatureValue = null;
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate);

        verify(validationPropertiesContainer).isSignatureRequired();
        verifyNoMoreInteractions(validationPropertiesContainer);
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityKey_fail_whenAuthenticityCheckNotOk() {
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = {1, 2, 3};
        byte[] signatureKey = {4, 5, 6};

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        when(subscriberCertificatesContainer.getCertificateWithSerialNumber(certificateSerialNumber)).thenReturn(certificateWithChainValidity);
        when(certificateAndSignatureVerifier.verifyValueSignature("my-service", bytesToValidate, signatureKey, certificateWithChainValidity))
                .thenReturn(false);

        assertThrows(MessageSignatureValidationException.class, () -> signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate));
    }

    @Test
    void checkAuthenticityKey_doFail_whenCertificateSetAndSignatureNotSet() {
        byte[] certificateSerialNumber = {7, 8, 9};
        byte[] signatureValue = null;
        byte[] signatureKey = null;

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        assertThrows(MessageSignatureValidationException.class, () -> signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired();
        verifyNoMoreInteractions(validationPropertiesContainer);
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityKey_doFail_whenSetSignatureSetAndCertificateNotSet() {
        byte[] certificateSerialNumber = null;
        byte[] signatureValue = null;
        byte[] signatureKey = {7, 8, 9};

        Headers headers = createHeaders(certificateSerialNumber, signatureValue, signatureKey);
        byte[] bytesToValidate = {1, 1, 1};

        assertThrows(MessageSignatureValidationException.class, () -> signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired();
        verifyNoMoreInteractions(validationPropertiesContainer);
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void checkAuthenticityKey_doFail_whenHeadersNotSet() {
        Headers headers = null;
        byte[] bytesToValidate = {1, 1, 1};

        assertThrows(MessageSignatureValidationException.class, () -> signatureAuthenticityService.checkAuthenticityKey(headers, bytesToValidate));

        verify(validationPropertiesContainer).isSignatureRequired();
        verifyNoMoreInteractions(validationPropertiesContainer);
        verifyNoInteractions(certificateAndSignatureVerifier);
    }

    @Test
    void init() {
        SignatureMetricsService signatureMetricsService = mock(SignatureMetricsService.class);
        signatureAuthenticityService = new DefaultSignatureAuthenticityService(validationPropertiesContainer,
                certificateAndSignatureVerifier, subscriberCertificatesContainer, Optional.of(signatureMetricsService));
        reset(validationPropertiesContainer);

        signatureAuthenticityService.init();

        verify(validationPropertiesContainer).isSignatureRequired();
    }

    @Test
    void init_withMetricsService() {
        SignatureMetricsService signatureMetricsService = mock(SignatureMetricsService.class);
        signatureAuthenticityService = new DefaultSignatureAuthenticityService(validationPropertiesContainer,
                certificateAndSignatureVerifier, subscriberCertificatesContainer, Optional.of(signatureMetricsService));
        reset(validationPropertiesContainer);

        signatureAuthenticityService.init();

        verify(validationPropertiesContainer).isSignatureRequired();
        verify(signatureMetricsService).initSignatureRequiredMetricName(any());
    }

    private Message createMessage(String messageTypeName, String service) {
        Message message = mock(Message.class);
        MessageType messageType = mock(MessageType.class);
        when(messageType.getName()).thenReturn(messageTypeName);
        MessagePublisher messagePublisher = mock(MessagePublisher.class);
        when(messagePublisher.getService()).thenReturn(service);

        when(message.getType()).thenReturn(messageType);
        when(message.getPublisher()).thenReturn(messagePublisher);

        return message;
    }

    private Headers createHeaders(byte[] certificateSerialNumber, byte[] signatureValue, byte[] signatureKey) {
        Headers headers = mock(Headers.class);
        if (certificateSerialNumber != null) {
            Header header = createHeader(certificateSerialNumber);
            when(headers.lastHeader(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY)).thenReturn(header);
        }
        if (signatureValue != null) {
            Header header = createHeader(signatureValue);
            when(headers.lastHeader(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY)).thenReturn(header);
        }
        if (signatureKey != null) {
            Header header = createHeader(signatureKey);
            when(headers.lastHeader(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY)).thenReturn(header);
        }
        return headers;
    }

    private Header createHeader(byte[] value) {
        Header header = mock(Header.class);
        when(header.value()).thenReturn(value);
        return header;
    }

}
