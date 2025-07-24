package ch.admin.bit.jeap.messaging.kafka.auth.aws.iam;

import ch.admin.bit.jeap.messaging.auth.aws.iam.util.IAMRolesAnywhereCredentialsProviderHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.msk.auth.iam.internals.AWSCredentialsCallback;

import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IAMRolesAnywhereCallbackHandlerTest {

    private IAMRolesAnywhereCallbackHandler handler;
    private AwsCredentialsProvider mockProvider;

    @BeforeEach
    void setUp() {
        handler = new IAMRolesAnywhereCallbackHandler();
        mockProvider = mock(AwsCredentialsProvider.class);
    }

    @Test
    void testConfigureWithValidMechanism() {
        try (MockedStatic<IAMRolesAnywhereCredentialsProviderHolder> mockedStatic = mockStatic(IAMRolesAnywhereCredentialsProviderHolder.class)) {
            mockedStatic.when(IAMRolesAnywhereCredentialsProviderHolder::getProvider).thenReturn(mockProvider);

            handler.configure(Map.of(), "AWS_MSK_IAM", Collections.emptyList());
        }
    }

    @Test
    void testConfigureWithInvalidMechanismThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                handler.configure(Map.of(), "INVALID_MECHANISM", Collections.emptyList()));
    }

    @Test
    void testHandleAWSCredentialsCallback() throws IOException, UnsupportedCallbackException {
        AwsCredentials mockCredentials = mock(AwsCredentials.class);
        when(mockProvider.resolveCredentials()).thenReturn(mockCredentials);

        try (MockedStatic<IAMRolesAnywhereCredentialsProviderHolder> mockedStatic = mockStatic(IAMRolesAnywhereCredentialsProviderHolder.class)) {
            mockedStatic.when(IAMRolesAnywhereCredentialsProviderHolder::getProvider).thenReturn(mockProvider);
            handler.configure(Map.of(), "AWS_MSK_IAM", Collections.emptyList());

            AWSCredentialsCallback callback = new AWSCredentialsCallback();
            handler.handle(new javax.security.auth.callback.Callback[]{callback});

            assertEquals(mockCredentials, callback.getAwsCredentials());
            assertNull(callback.getLoadingException());
        }
    }

    @Test
    void testHandleUnsupportedCallbackThrowsException() {
        javax.security.auth.callback.Callback unsupportedCallback = mock(javax.security.auth.callback.Callback.class);

        assertThrows(UnsupportedCallbackException.class, () ->
                handler.handle(new javax.security.auth.callback.Callback[]{unsupportedCallback}));
    }

    @Test
    void testHandleWithMixedCallbacksThrowsOnUnsupported() {
        AwsCredentials mockCredentials = mock(AwsCredentials.class);
        when(mockProvider.resolveCredentials()).thenReturn(mockCredentials);

        try (MockedStatic<IAMRolesAnywhereCredentialsProviderHolder> mockedStatic = mockStatic(IAMRolesAnywhereCredentialsProviderHolder.class)) {
            mockedStatic.when(IAMRolesAnywhereCredentialsProviderHolder::getProvider).thenReturn(mockProvider);
            handler.configure(Map.of(), "AWS_MSK_IAM", Collections.emptyList());

            AWSCredentialsCallback supported = new AWSCredentialsCallback();
            javax.security.auth.callback.Callback unsupported = mock(javax.security.auth.callback.Callback.class);

            UnsupportedCallbackException exception = assertThrows(UnsupportedCallbackException.class, () ->
                    handler.handle(new javax.security.auth.callback.Callback[]{supported, unsupported}));

            assertNotNull(exception);
        }
    }
}
