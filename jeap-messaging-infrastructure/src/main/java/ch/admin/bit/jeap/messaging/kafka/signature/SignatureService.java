package ch.admin.bit.jeap.messaging.kafka.signature;

import org.apache.kafka.common.header.Headers;

public interface SignatureService {

    void injectSignature(Headers headers, byte[] bytesToSign, boolean isKey);
}
