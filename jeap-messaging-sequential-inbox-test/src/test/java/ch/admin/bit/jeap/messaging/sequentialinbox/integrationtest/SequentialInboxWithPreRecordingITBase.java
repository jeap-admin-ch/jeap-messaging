package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

class SequentialInboxWithPreRecordingITBase extends SequentialInboxITBase {

    private static final LocalDateTime PAST_TIMESTAMP_WHERE_SEQUENCING_STARTED = LocalDateTime.now().minusDays(1);
    private static final LocalDateTime FUTURE_TIMESTAMP_WHERE_RECORDING_IS_ENABLED = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUpRecording() {
        givenRecordingIsConfigured();
    }

    protected void givenRecordingIsConfigured() {
        sequentialInboxService.sequencingStartTimestamp = FUTURE_TIMESTAMP_WHERE_RECORDING_IS_ENABLED;
    }

    protected void whenRecordingEndsAndSequencingStarts() {
        sequentialInboxService.sequencingStartTimestamp = PAST_TIMESTAMP_WHERE_SEQUENCING_STARTED;
    }

    @AfterEach
    void disableRecordingMode () {
        sequentialInboxService.sequencingStartTimestamp = null;
    }
}
