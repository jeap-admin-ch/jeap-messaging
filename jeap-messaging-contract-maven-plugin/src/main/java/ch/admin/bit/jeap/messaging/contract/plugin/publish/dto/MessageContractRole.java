package ch.admin.bit.jeap.messaging.contract.plugin.publish.dto;

public enum MessageContractRole {
    CONSUMER,
    PRODUCER;

    public static MessageContractRole fromString(String role) {
        for (MessageContractRole value : values()) {
            if (value.name().equalsIgnoreCase(role)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown messaging participant role: " + role);
    }
}
