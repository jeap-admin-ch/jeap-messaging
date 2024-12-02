package ch.admin.bit.jeap.messaging.transactionaloutbox.jpa;

import ch.admin.bit.jeap.messaging.transactionaloutbox.outbox.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Transactional
@DataJpaTest
@ContextConfiguration(classes = OutboxJpaConfig.class)
class JpaDeferredMessageRepositoryTest {

    private static final byte[] MESSAGE = "test-message".getBytes(StandardCharsets.UTF_8);
    private static final byte[] KEY = "test-key".getBytes(StandardCharsets.UTF_8);
    private static final String TOPIC = "test-topic";
    public static final String TEST_MESSAGE = "TestMessage";
    public static final String VERSION = "1.0.0";

    @Autowired
    private JpaDeferredMessageRepository jpaDeferredMessageRepository;

    @Autowired
    private SpringDataJpaDeferredMessageRepository springDataJpaDeferredMessageRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    void testSave() {
        final ZonedDateTime beforeCreation = ZonedDateTime.now();
        final DeferredMessage deferredMessage = createTestMessage(true);

        final DeferredMessage persistedDeferredMessage = jpaDeferredMessageRepository.save(deferredMessage);

        DeferredMessage foundPersistedDeferredMessage = flushDetachAndFind(persistedDeferredMessage);
        assertThat(foundPersistedDeferredMessage.getMessage()).isEqualTo(MESSAGE);
        assertThat(foundPersistedDeferredMessage.getKey()).isEqualTo(KEY);
        assertThat(foundPersistedDeferredMessage.getTopic()).isEqualTo(TOPIC);
        assertThat(foundPersistedDeferredMessage.isSendImmediately()).isTrue();
        assertThat(foundPersistedDeferredMessage.getCreated()).isAfterOrEqualTo(beforeCreation);
        assertThat(foundPersistedDeferredMessage.getCreated()).isBeforeOrEqualTo(ZonedDateTime.now());
        assertThat(foundPersistedDeferredMessage.getSentImmediately()).isNull();
        assertThat(foundPersistedDeferredMessage.getSentScheduled()).isNull();
        assertThat(foundPersistedDeferredMessage.getScheduleAfter()).isNull();
        assertThat(foundPersistedDeferredMessage.getFailed()).isNull();
        assertThat(foundPersistedDeferredMessage.getMessageTypeName()).isEqualTo(TEST_MESSAGE);
        assertThat(foundPersistedDeferredMessage.getMessageTypeVersion()).isEqualTo(VERSION);
    }

    @Test
    void testMarkSentImmediately() {
        final DeferredMessage deferredMessage = createTestMessage(true);
        final DeferredMessage persistedDeferredMessage = jpaDeferredMessageRepository.save(deferredMessage);
        testEntityManager.flush();
        final ZonedDateTime sentTime = truncateToMillis(ZonedDateTime.now());

        jpaDeferredMessageRepository.markSentImmediately(persistedDeferredMessage.getId(), sentTime);

        DeferredMessage foundPersistedDeferredMessage = flushDetachAndFind(persistedDeferredMessage);
        assertThat(foundPersistedDeferredMessage.getSentImmediately()).isEqualTo(sentTime);
        assertThat(foundPersistedDeferredMessage.getSentScheduled()).isNull();
        assertThat(foundPersistedDeferredMessage.getFailed()).isNull();

        assertThatThrownBy( () -> jpaDeferredMessageRepository.markSentImmediately(42, sentTime))
                .isInstanceOf(TransactionalOutboxException.class)
                .hasMessageContaining("42");
    }

    @Test
    void testMarkSentScheduled() {
        final DeferredMessage deferredMessage = createTestMessage(false);
        final DeferredMessage persistedDeferredMessage = jpaDeferredMessageRepository.save(deferredMessage);
        testEntityManager.flush();
        final ZonedDateTime sentTime = truncateToMillis(ZonedDateTime.now());

        jpaDeferredMessageRepository.markSentScheduled(persistedDeferredMessage.getId(), sentTime);

        DeferredMessage foundPersistedDeferredMessage = flushDetachAndFind(persistedDeferredMessage);
        assertThat(foundPersistedDeferredMessage.getSentScheduled()).isEqualTo(sentTime);
        assertThat(foundPersistedDeferredMessage.getSentImmediately()).isNull();
        assertThat(foundPersistedDeferredMessage.getFailed()).isNull();

        assertThatThrownBy( () -> jpaDeferredMessageRepository.markSentScheduled(42, sentTime))
                .isInstanceOf(TransactionalOutboxException.class)
                .hasMessageContaining("42");
    }

    @Test
    void testMarkFailed() {
        final DeferredMessage deferredMessage = createTestMessage(false);
        final DeferredMessage persistedDeferredMessage = jpaDeferredMessageRepository.save(deferredMessage);
        testEntityManager.flush();
        final ZonedDateTime failedTime = truncateToMillis(ZonedDateTime.now());

        jpaDeferredMessageRepository.markFailed(persistedDeferredMessage.getId(), failedTime, SendFailureReason.UNAUTHORIZED_ON_TOPIC);

        DeferredMessage foundPersistedDeferredMessage = flushDetachAndFind(persistedDeferredMessage);
        assertThat(foundPersistedDeferredMessage.getFailed()).isEqualTo(failedTime);
        assertThat(foundPersistedDeferredMessage.getFailReason()).isEqualTo(SendFailureReason.UNAUTHORIZED_ON_TOPIC);
        assertThat(foundPersistedDeferredMessage.getSentScheduled()).isNull();
        assertThat(foundPersistedDeferredMessage.getSentImmediately()).isNull();

        assertThatThrownBy( () -> jpaDeferredMessageRepository.markFailed(42, failedTime, SendFailureReason.UNAUTHORIZED_ON_TOPIC))
                .isInstanceOf(TransactionalOutboxException.class)
                .hasMessageContaining("42");
    }

    @Test
    void testSetScheduleAfter() {
        final DeferredMessage deferredMessage = createTestMessage(true);
        final DeferredMessage persistedDeferredMessage = jpaDeferredMessageRepository.save(deferredMessage);
        testEntityManager.flush();
        final ZonedDateTime scheduleAfter = truncateToMillis(ZonedDateTime.now());

        jpaDeferredMessageRepository.setScheduleAfter(persistedDeferredMessage.getId(), scheduleAfter);

        DeferredMessage foundPersistedDeferredMessage = flushDetachAndFind(persistedDeferredMessage);
        assertThat(foundPersistedDeferredMessage.getScheduleAfter()).isEqualTo(scheduleAfter);

        assertThatThrownBy( () -> jpaDeferredMessageRepository.setScheduleAfter(42, scheduleAfter))
                .isInstanceOf(TransactionalOutboxException.class)
                .hasMessageContaining("42");
    }

    @Test
    void testMarkForResend() {
        final DeferredMessage deferredMessage = createTestMessage(false);
        final DeferredMessage persistedDeferredMessage = jpaDeferredMessageRepository.save(deferredMessage);
        jpaDeferredMessageRepository.markFailed(persistedDeferredMessage.getId(), ZonedDateTime.now(), SendFailureReason.UNAUTHORIZED_ON_TOPIC);
        DeferredMessage failedDeferredMessage = flushDetachAndFind(persistedDeferredMessage);
        assertThat(failedDeferredMessage.isResend()).isFalse();

        jpaDeferredMessageRepository.markForResend(failedDeferredMessage.getId(), true);

        DeferredMessage resendDeferredMessage = flushDetachAndFind(failedDeferredMessage);
        assertThat(resendDeferredMessage.isResend()).isTrue();

        assertThatThrownBy( () ->  jpaDeferredMessageRepository.markForResend(42, true))
                .isInstanceOf(TransactionalOutboxException.class)
                .hasMessageContaining("42");
    }

    @Test
    void testFindMessagesReadyToBeSentNumMessages() {
        assertThat(jpaDeferredMessageRepository.findMessagesReadyToBeSent(1)).isEmpty();
        final DeferredMessage deferredMessage1 = createTestMessage(false);
        final DeferredMessage deferredMessage2 = createTestMessage(false);
        final DeferredMessage deferredMessage3 = createTestMessage(false);
        jpaDeferredMessageRepository.save(deferredMessage1);
        jpaDeferredMessageRepository.save(deferredMessage2);
        jpaDeferredMessageRepository.save(deferredMessage3);
        testEntityManager.flush();

        List<DeferredMessage> readyToBeSentDeferredMessages = jpaDeferredMessageRepository.findMessagesReadyToBeSent(2);

        assertThat(readyToBeSentDeferredMessages).hasSize(2);
        assertThat(readyToBeSentDeferredMessages.get(0).getId()).isEqualTo(deferredMessage1.getId());
        assertThat(readyToBeSentDeferredMessages.get(1).getId()).isEqualTo(deferredMessage2.getId());

        List<DeferredMessage> allReadyToBeSentDeferredMessages = jpaDeferredMessageRepository.findMessagesReadyToBeSent(10);
        assertThat(allReadyToBeSentDeferredMessages).hasSize(3);
        assertThat(allReadyToBeSentDeferredMessages.get(0).getId()).isEqualTo(deferredMessage1.getId());
        assertThat(allReadyToBeSentDeferredMessages.get(1).getId()).isEqualTo(deferredMessage2.getId());
        assertThat(allReadyToBeSentDeferredMessages.get(2).getId()).isEqualTo(deferredMessage3.getId());
    }

    @Test
    void testFindCountMessagesReadyToBeSent() {
        assertThat(jpaDeferredMessageRepository.countMessagesReadyToBeSent()).isEqualTo(0);
        assertThat(jpaDeferredMessageRepository.findMessagesReadyToBeSent(1)).isEmpty();
        final DeferredMessage deferredMessage1 = createTestMessage(false);
        final DeferredMessage deferredMessage2 = createTestMessage(false);
        final DeferredMessage deferredMessage3 = createTestMessage(true);
        final DeferredMessage deferredMessage4 = createTestMessage(true);
        final DeferredMessage deferredMessage5 = createTestMessage(true);
        final DeferredMessage deferredMessage6 = createTestMessage(false);
        final DeferredMessage deferredMessage7 = createTestMessage(false);
        final Duration relayDelay = Duration.ofSeconds(2);

        jpaDeferredMessageRepository.save(deferredMessage1);
        jpaDeferredMessageRepository.save(deferredMessage2);
        jpaDeferredMessageRepository.save(deferredMessage3);
        jpaDeferredMessageRepository.save(deferredMessage4);
        jpaDeferredMessageRepository.save(deferredMessage5);
        jpaDeferredMessageRepository.save(deferredMessage6);
        jpaDeferredMessageRepository.save(deferredMessage7);
        jpaDeferredMessageRepository.markSentScheduled(deferredMessage2.getId(), ZonedDateTime.now());
        jpaDeferredMessageRepository.setScheduleAfter(deferredMessage3.getId(), ZonedDateTime.now().plus(relayDelay));
        jpaDeferredMessageRepository.markSentImmediately(deferredMessage3.getId(), ZonedDateTime.now());
        jpaDeferredMessageRepository.setScheduleAfter(deferredMessage4.getId(), ZonedDateTime.now().plus(relayDelay));
        jpaDeferredMessageRepository.setScheduleAfter(deferredMessage5.getId(), ZonedDateTime.now().minusSeconds(1));
        jpaDeferredMessageRepository.markFailed(deferredMessage6.getId(), ZonedDateTime.now(), SendFailureReason.UNAUTHORIZED_ON_TOPIC);
        jpaDeferredMessageRepository.markFailed(deferredMessage7.getId(), ZonedDateTime.now(), SendFailureReason.UNAUTHORIZED_ON_TOPIC);
        jpaDeferredMessageRepository.markForResend(deferredMessage7.getId(), true);

        testEntityManager.flush();

        final int numMessagesReadyToBeSent = jpaDeferredMessageRepository.countMessagesReadyToBeSent();
        final List<DeferredMessage> readyToBeSentDeferredMessages = jpaDeferredMessageRepository.findMessagesReadyToBeSent(10);

        assertThat(numMessagesReadyToBeSent).isEqualTo(3);
        assertThat(readyToBeSentDeferredMessages).hasSize(3);
        assertThat(readyToBeSentDeferredMessages.get(0).getId()).isEqualTo(deferredMessage1.getId());
        assertThat(readyToBeSentDeferredMessages.get(1).getId()).isEqualTo(deferredMessage5.getId());
        assertThat(readyToBeSentDeferredMessages.get(2).getId()).isEqualTo(deferredMessage7.getId());
    }

    @Test
    void testCountFailedMessages() {
        final ZonedDateTime failedStartFrom = ZonedDateTime.now();
        final ZonedDateTime failedBefore = failedStartFrom.plusMinutes(3);

        assertThat(jpaDeferredMessageRepository.countFailedMessages(false)).isEqualTo(0);
        assertThat(jpaDeferredMessageRepository.countFailedMessages(true)).isEqualTo(0);
        assertThat(jpaDeferredMessageRepository.countFailedMessages(failedStartFrom, failedBefore, false)).isEqualTo(0);
        assertThat(jpaDeferredMessageRepository.countFailedMessages(failedStartFrom, failedBefore, true)).isEqualTo(0);

        createAndSaveDeferredMessage(true, null, null, false);
        createAndSaveDeferredMessage(true, null, null, true);

        createAndSaveDeferredMessage(true, failedStartFrom.minusMinutes(1), true, false);
        createAndSaveDeferredMessage(true, failedStartFrom.minusMinutes(1), true, true);

        createAndSaveDeferredMessage(true, failedStartFrom, true, false);
        createAndSaveDeferredMessage(true, failedStartFrom, true, true);

        createAndSaveDeferredMessage(false, failedStartFrom.plusMinutes(1), true, false);
        createAndSaveDeferredMessage(false, failedStartFrom.plusMinutes(1), true, true);

        createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(1), false, false);
        createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(1), false, true);

        createAndSaveDeferredMessage(true, failedBefore, true, false);
        createAndSaveDeferredMessage(true, failedBefore, true, true);

        assertThat(jpaDeferredMessageRepository.countFailedMessages(false)).isEqualTo(4);
        assertThat(jpaDeferredMessageRepository.countFailedMessages(true)).isEqualTo(4);
        assertThat(jpaDeferredMessageRepository.countFailedMessages(failedStartFrom, failedBefore, false)).isEqualTo(2);
        assertThat(jpaDeferredMessageRepository.countFailedMessages(failedStartFrom, failedBefore, true)).isEqualTo(2);
    }

    @Test
    void testFindFailedMessages() {
        final ZonedDateTime failedStartFrom = truncateToMillis(ZonedDateTime.now());
        final ZonedDateTime failedBefore = failedStartFrom.plusMinutes(4);

        assertThat(jpaDeferredMessageRepository.findFailedMessages(failedStartFrom, failedBefore, false, 10)).isEmpty();
        assertThat(jpaDeferredMessageRepository.findFailedMessages(1, failedBefore, true, 10)).isEmpty();

        createAndSaveDeferredMessage(true, null, null, false);
        createAndSaveDeferredMessage(true, null, null, true);

        createAndSaveDeferredMessage(true, failedStartFrom.minusMinutes(1), true, false);
        createAndSaveDeferredMessage(true, failedStartFrom.minusMinutes(1), true, true);

        FailedMessage fm1 = FailedMessage.from(createAndSaveDeferredMessage(true, failedStartFrom, true, false));
        FailedMessage fm1r = FailedMessage.from(createAndSaveDeferredMessage(true, failedStartFrom, true, true));

        createAndSaveDeferredMessage(false, failedStartFrom.plusMinutes(1), false, false);
        createAndSaveDeferredMessage(false, failedStartFrom.plusMinutes(1), false, true);

        FailedMessage fm2 = FailedMessage.from(createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(1), true, false));
        FailedMessage fm2r = FailedMessage.from(createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(1), true, true));

        FailedMessage fm3 = FailedMessage.from(createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(2), true, false));
        FailedMessage fm3r = FailedMessage.from(createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(2), true, true));

        createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(3), true, false);
        createAndSaveDeferredMessage(true, failedStartFrom.plusMinutes(3), true, true);

        createAndSaveDeferredMessage(true, failedBefore, true, false);
        createAndSaveDeferredMessage(true, failedBefore, true, true);

        assertThat(jpaDeferredMessageRepository.findFailedMessages(failedStartFrom, failedBefore, false, 2))
            .containsExactly(fm1, fm2);
        assertThat(jpaDeferredMessageRepository.findFailedMessages(failedStartFrom, failedBefore, true, 2))
                .containsExactly(fm1r, fm2r);
        assertThat(jpaDeferredMessageRepository.findFailedMessages(fm1.getId(), failedBefore, false, 2))
            .containsExactly(fm2, fm3);
        assertThat(jpaDeferredMessageRepository.findFailedMessages(fm1r.getId(), failedBefore, true, 2))
                .containsExactly(fm2r, fm3r);
    }

    @SneakyThrows
    @Test
    void testDelete() {
        assertThat(springDataJpaDeferredMessageRepository.findAll()).isEmpty();
        final ZonedDateTime markSentDateTime = ZonedDateTime.now();
        final ZonedDateTime deleteBeforeDateTime = markSentDateTime.minusMinutes(1);
        final DeferredMessage deferredMessage1 = createTestMessage(false);
        final DeferredMessage deferredMessage2 = createTestMessage(true);
        final DeferredMessage deferredMessage3 = createTestMessage(false);
        final DeferredMessage deferredMessage4 = createTestMessage(true);
        final DeferredMessage deferredMessage5 = createTestMessage(false);
        Thread.sleep(1000); // Creation timestamp of message 6 will be 1s later than the creation timestamps of messages 1 to 5
        final DeferredMessage deferredMessage6 = createTestMessage(true);
        final ZonedDateTime afterMessage6Created = ZonedDateTime.now();

        jpaDeferredMessageRepository.save(deferredMessage1);
        jpaDeferredMessageRepository.save(deferredMessage2);
        jpaDeferredMessageRepository.save(deferredMessage3);
        jpaDeferredMessageRepository.save(deferredMessage4);
        jpaDeferredMessageRepository.save(deferredMessage5);
        jpaDeferredMessageRepository.save(deferredMessage6);
        jpaDeferredMessageRepository.markSentScheduled(deferredMessage1.getId(), markSentDateTime);
        jpaDeferredMessageRepository.markSentImmediately(deferredMessage2.getId(), markSentDateTime);
        jpaDeferredMessageRepository.markSentScheduled(deferredMessage3.getId(), deleteBeforeDateTime.minusSeconds(1));
        jpaDeferredMessageRepository.markSentImmediately(deferredMessage4.getId(), deleteBeforeDateTime.minusSeconds(1));
        testEntityManager.flush();

        int numSentDeleted = jpaDeferredMessageRepository.deleteMessagesSentBefore(deleteBeforeDateTime);

        assertThat(numSentDeleted).isEqualTo(2);
        List<DeferredMessage> allRemainingMessages = springDataJpaDeferredMessageRepository.findAll();
        assertThat(allRemainingMessages.stream().map(DeferredMessage::getId).collect(Collectors.toList()))
                .containsOnly(deferredMessage1.getId(), deferredMessage2.getId(), deferredMessage5.getId(), deferredMessage6.getId());

        int numUnsentDeleted = jpaDeferredMessageRepository.deleteUnsentMessagesCreatedBefore(afterMessage6Created.minus(Duration.ofMillis(500)));

        assertThat(numUnsentDeleted).isEqualTo(1);
        allRemainingMessages = springDataJpaDeferredMessageRepository.findAll();
        assertThat(allRemainingMessages.stream().map(DeferredMessage::getId).collect(Collectors.toList()))
                .containsOnly(deferredMessage1.getId(), deferredMessage2.getId(), deferredMessage6.getId());
    }

    private DeferredMessage createAndSaveDeferredMessage(boolean sendImmediately, ZonedDateTime failedOrSucceededAt, Boolean failed, boolean resend) {
        long deferredMessageId = jpaDeferredMessageRepository.save(createTestMessage(sendImmediately)).getId();
        if (failed == null) {
            assertThat(failedOrSucceededAt).isNull();
        }
        else {
            assertThat(failed).isNotNull();
            if (failed) {
                jpaDeferredMessageRepository.markFailed(deferredMessageId, failedOrSucceededAt, SendFailureReason.UNAUTHORIZED_ON_TOPIC);
            } else {
                if (sendImmediately) {
                    jpaDeferredMessageRepository.markSentImmediately(deferredMessageId, failedOrSucceededAt);
                } else {
                    jpaDeferredMessageRepository.markSentScheduled(deferredMessageId, failedOrSucceededAt);
                }
            }
        }
        jpaDeferredMessageRepository.markForResend(deferredMessageId, resend);
        testEntityManager.flush();
        testEntityManager.clear();
        return springDataJpaDeferredMessageRepository.getReferenceById(deferredMessageId);
    }

    private DeferredMessage createTestMessage(boolean sendImmediately) {
        return DeferredMessage.builder()
                .message(MESSAGE)
                .key(KEY)
                .topic(TOPIC)
                .sendImmediately(sendImmediately)
                .messageId(UUID.randomUUID().toString())
                .messageIdempotenceId(UUID.randomUUID().toString())
                .messageTypeName(TEST_MESSAGE)
                .messageTypeVersion(VERSION)
                .traceContext(OutboxTraceContext.builder().traceId(1L).spanId(1L).parentSpanId(1L).build())
                .build();
    }

    private DeferredMessage flushDetachAndFind(DeferredMessage deferredMessage) {
        testEntityManager.flush();
        testEntityManager.detach(deferredMessage);
        return testEntityManager.find(DeferredMessage.class, deferredMessage.getId());
    }

    // As the database is only able to store milliseconds precision we have to truncate ZondeDateTime values to milliseconds to be able
    // to do an equals comparison between ZondeDateTime instances sent to the database and the date time data read back from the database.
    private ZonedDateTime truncateToMillis(ZonedDateTime zonedDateTime) {
        return zonedDateTime.truncatedTo(ChronoUnit.MILLIS);
    }
}
