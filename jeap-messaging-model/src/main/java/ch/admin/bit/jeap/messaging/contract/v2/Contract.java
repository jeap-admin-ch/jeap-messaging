package ch.admin.bit.jeap.messaging.contract.v2;

import lombok.*;

@Value
@Builder
@AllArgsConstructor
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
    /** Schema filename; null for legacy contracts, empty if generation could not supply a schema. */
    String schemaResource;

    /** Retains source and binary compatibility with contracts created before schema references. */
    public Contract(String contractVersion, String role, String systemName, String messageTypeName,
                    String messageTypeVersion, String registryUrl, String registryBranch, String registryCommit,
                    String compatibilityMode, String appName, String[] topics, String encryptionKeyId) {
        this(contractVersion, role, systemName, messageTypeName, messageTypeVersion, registryUrl, registryBranch,
                registryCommit, compatibilityMode, appName, topics, encryptionKeyId, null);
    }
}
