package ch.admin.bit.jeap.messaging.kafka.signature;

import org.apache.kafka.common.header.Headers;

public class SignatureInjector {

    private final ByteSigner byteSigner;
    private final byte[] certificateSerialNumber;

    SignatureInjector(ByteSigner byteSigner, byte[] certificateSerialNumber) {
        this.byteSigner = byteSigner;
        this.certificateSerialNumber = certificateSerialNumber;
    }

    public void injectSignature(Headers headers, byte[] bytesToSign, boolean isKey) {
        byte[] signature = byteSigner.createSignature(bytesToSign);
        headers.add(determineHeaderKey(isKey), signature);
        if (!isKey) {
            headers.add(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, certificateSerialNumber);
        }
    }

    private String determineHeaderKey(boolean isKey) {
        return isKey ? SignatureHeaders.SIGNATURE_KEY_HEADER_KEY : SignatureHeaders.SIGNATURE_PAYLOAD_HEADER_KEY;
    }
}
