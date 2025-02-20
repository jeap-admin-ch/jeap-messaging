package ch.admin.bit.jeap.messaging.kafka.signature;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SignatureHeaders {
    public static final String SIGNATURE_VALUE_HEADER_KEY = "jeap-sign";
    public static final String SIGNATURE_KEY_HEADER_KEY = "jeap-sign-key";
    public static final String SIGNATURE_CERTIFICATE_HEADER_KEY = "jeap-cert";
}
