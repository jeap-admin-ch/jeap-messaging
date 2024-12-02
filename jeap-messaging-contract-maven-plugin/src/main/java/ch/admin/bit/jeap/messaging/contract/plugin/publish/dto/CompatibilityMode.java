package ch.admin.bit.jeap.messaging.contract.plugin.publish.dto;

public enum CompatibilityMode {
    BACKWARD,
    BACKWARD_TRANSITIVE,
    FORWARD,
    FORWARD_TRANSITIVE,
    FULL,
    FULL_TRANSITIVE,
    NONE;

    public static CompatibilityMode fromString(String compatibilityMode) {
        for (CompatibilityMode value : values()) {
            if (value.name().equalsIgnoreCase(compatibilityMode)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CompatibilityMode: " + compatibilityMode);
    }
}
