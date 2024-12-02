package ch.admin.bit.jeap.messaging.registry.dto;

public enum CompatibilityMode {
    BACKWARD,
    FORWARD,
    FULL,
    NONE;

    public boolean isForwardOrFull() {
        return this == FORWARD || this == FULL;
    }

    public boolean isBackwardOrFull() {
        return this == BACKWARD || this == FULL;
    }
}
