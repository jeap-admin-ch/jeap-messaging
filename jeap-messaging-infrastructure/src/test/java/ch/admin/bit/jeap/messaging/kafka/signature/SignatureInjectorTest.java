package ch.admin.bit.jeap.messaging.kafka.signature;

import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignatureInjectorTest {

    private static final byte[] CERTIFICATE_SERIAL_NUMBER = {1, 2, 3};
    private static final byte[] BYTES_TO_SIGN = {4, 5, 6};
    private static final byte[] SIGNATURE = {7, 8, 9};

    @Mock
    private ByteSigner byteSigner;

    private SignatureInjector signatureInjector;

    @BeforeEach
    void setUp() {
        when(byteSigner.createSignature(BYTES_TO_SIGN)).thenReturn(SIGNATURE);
        signatureInjector = new SignatureInjector(byteSigner, CERTIFICATE_SERIAL_NUMBER);
    }

    @Test
    void injectSignature_value() {
        Headers headers = mock(Headers.class);

        signatureInjector.injectSignature(headers, BYTES_TO_SIGN, false);

        verify(byteSigner, times(1)).createSignature(BYTES_TO_SIGN);
        verify(headers, times(1)).add("jeap-sign", SIGNATURE);
        verify(headers, times(1)).add("jeap-cert", CERTIFICATE_SERIAL_NUMBER);
        verifyNoMoreInteractions(byteSigner, headers);
    }

    @Test
    void injectSignature_key() {
        Headers headers = mock(Headers.class);

        signatureInjector.injectSignature(headers, BYTES_TO_SIGN, true);

        verify(byteSigner, times(1)).createSignature(BYTES_TO_SIGN);
        verify(headers, times(1)).add("jeap-sign-key", SIGNATURE);
        verifyNoMoreInteractions(byteSigner, headers);
    }

}