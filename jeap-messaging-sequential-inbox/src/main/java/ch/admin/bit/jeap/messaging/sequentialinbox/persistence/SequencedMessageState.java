package ch.admin.bit.jeap.messaging.sequentialinbox.persistence;

public enum SequencedMessageState {
    WAITING, PROCESSED, FAILED;

    public static boolean waitingOrProcessed(SequencedMessageState state) {
        return state == WAITING || state == PROCESSED;
    }
}
