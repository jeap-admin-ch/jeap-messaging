package ch.admin.bit.jeap.messaging.kafka.signature;

import org.apache.kafka.common.header.Headers;

public class SignatureInjector {

    private static final String SIGNATURE_PAYLOAD_HEADER_KEY = "jeap-sign";
    private static final String SIGNATURE_KEY_HEADER_KEY = "jeap-sign-key";
    private static final String SIGNATURE_CERTIFICATE_HEADER_KEY = "jeap-cert";

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
            headers.add(SIGNATURE_CERTIFICATE_HEADER_KEY, certificateSerialNumber);
        }
    }

    private String determineHeaderKey(boolean isKey) {
        return isKey ? SIGNATURE_KEY_HEADER_KEY : SIGNATURE_PAYLOAD_HEADER_KEY;
    }
}