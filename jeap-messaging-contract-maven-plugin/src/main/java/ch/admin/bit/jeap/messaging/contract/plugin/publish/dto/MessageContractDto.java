package ch.admin.bit.jeap.messaging.contract.plugin.publish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class MessageContractDto {
    String appName;
    String messageType;
    String messageTypeVersion;
    String topic;
    MessageContractRole role;
    String registryUrl;
    String commitHash;
    String branch;
    CompatibilityMode compatibilityMode;
    String encryptionKeyId;
}
