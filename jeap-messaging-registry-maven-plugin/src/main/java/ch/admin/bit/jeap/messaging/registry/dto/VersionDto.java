package ch.admin.bit.jeap.messaging.registry.dto;

import lombok.Data;

@Data
public class VersionDto {
    String version;
    String valueSchema;
    String keySchema;
    CompatibilityMode compatibilityMode;
    String compatibleVersion;
}
