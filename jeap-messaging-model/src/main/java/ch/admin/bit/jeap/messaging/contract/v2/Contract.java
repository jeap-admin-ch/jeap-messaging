package ch.admin.bit.jeap.messaging.contract.v2;

import lombok.*;

@Value
@Builder
public class Contract {
    String contractVersion;
    String role;
    String systemName;
    String messageTypeName;
    String messageTypeVersion;
    String registryUrl;
    String registryBranch;
    String registryCommit;
    String compatibilityMode;
    String appName;
    String[] topics;
    String encryptionKeyId;
}
