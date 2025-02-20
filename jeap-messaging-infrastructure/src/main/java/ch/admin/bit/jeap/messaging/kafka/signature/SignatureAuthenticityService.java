package ch.admin.bit.jeap.messaging.kafka.signature;

import org.apache.kafka.common.header.Headers;

public interface SignatureAuthenticityService {

    /**
     * Check the authenticity of the value, we can't check things like service name, because we don't know it.
     * @param headers the headers of the message
     * @param bytesToValidate the bytes to validate
     */
    void checkAuthenticityValue(Object deserialized, Headers headers, byte[] bytesToValidate);

    /**
     * Check the authenticity of the key, we can't check things like service name, because we don't know it.
     * @param headers the headers of the message
     * @param bytesToValidate the bytes to validate
     */
    void checkAuthenticityKey(Headers headers, byte[] bytesToValidate);
}
