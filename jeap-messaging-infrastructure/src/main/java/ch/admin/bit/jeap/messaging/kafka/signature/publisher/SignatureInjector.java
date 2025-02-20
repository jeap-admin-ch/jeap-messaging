package ch.admin.bit.jeap.messaging.kafka.signature.publisher;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import org.apache.kafka.common.header.Headers;

class SignatureInjector {

    private final ByteSigner byteSigner;
    private final byte[] certificateSerialNumber;

    SignatureInjector(ByteSigner byteSigner, byte[] certificateSerialNumber) {
        this.byteSigner = byteSigner;
        this.certificateSerialNumber = certificateSerialNumber;
    }

    void injectSignature(Headers headers, byte[] bytesToSign, boolean isKey) {
        byte[] signature = byteSigner.createSignature(bytesToSign);
        headers.add(determineHeaderKey(isKey), signature);
        if (!isKey) {
            headers.add(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, certificateSerialNumber);
        }
    }

    private String determineHeaderKey(boolean isKey) {
        return isKey ? SignatureHeaders.SIGNATURE_KEY_HEADER_KEY : SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY;
    }
}
