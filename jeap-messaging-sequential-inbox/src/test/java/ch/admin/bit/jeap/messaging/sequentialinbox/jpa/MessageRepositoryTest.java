package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = MessageRepositoryTest.TestConfig.class)
class MessageRepositoryTest {

    @EnableJpaRepositories
    @EntityScan(basePackageClasses = SequencedMessage.class)
    @ComponentScan
    static class TestConfig {
    }

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private SequenceInstanceRepository sequenceInstanceRepository;
    @Autowired
    private SpringDataJpaSequencedMessageRepository sequencedMessageRepository;

    @Autowired
    TestEntityManager testEntityManager;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("delete from buffered_message");
        jdbcTemplate.execute("delete from sequenced_message");
        jdbcTemplate.execute("delete from sequence_instance");
    }

    @Test
    void saveMessage_withBufferedMessage() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();
        BufferedMessage bufferedMessage = BufferedMessage.builder()
                .sequenceInstanceId(1L)
                .key(new byte[]{1, 2, 3})
                .value(new byte[]{4, 5, 6})
                .sequenceInstanceId(sequenceInstance.getId())
                .build();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(1L)
                .messageType("type")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .sequenceInstanceId(sequenceInstance.getId())
                .build();

        messageRepository.saveMessage(bufferedMessage, sequencedMessage);

        assertThat(bufferedMessage.getSequencedMessageId())
                .isEqualTo(sequencedMessage.getId());
        assertThat(testEntityManager.find(BufferedMessage.class, bufferedMessage.getId()))
                .isNotNull();
        assertThat(testEntityManager.find(SequencedMessage.class, sequencedMessage.getId()))
                .isNotNull();
    }

    @Test
    void saveMessage_withoutBufferedMessage() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(1L)
                .messageType("type")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .sequenceInstanceId(sequenceInstance.getId())
                .build();

        messageRepository.saveMessage(null, sequencedMessage);

        assertThat(testEntityManager.find(SequencedMessage.class, sequencedMessage.getId()))
                .isNotNull();
    }

    @Test
    void getProcessedMessageTypesInSequenceInNewTransaction_returnsCorrectTypes() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();

        SequencedMessage sequencedMessage1 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstance.getId())
                .messageType("type1")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId1")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.PROCESSED)
                .build();
        SequencedMessage sequencedMessage2 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstance.getId())
                .messageType("type2")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId2")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.PROCESSED)
                .build();
        messageRepository.saveMessage(null, sequencedMessage1);
        messageRepository.saveMessage(null, sequencedMessage2);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Set<String> result = messageRepository.getProcessedMessageTypesInSequenceInNewTransaction(sequenceInstance);

        assertThat(result).contains("type1", "type2");
    }

    @Test
    void getWaitingAndProcessedMessagesInNewTransaction_returnsCorrectMessages() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();

        SequencedMessage sequencedMessage1 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstance.getId())
                .messageType("type1")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId1")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .build();
        SequencedMessage sequencedMessage2 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstance.getId())
                .messageType("type2")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId2")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.PROCESSED)
                .build();
        messageRepository.saveMessage(null, sequencedMessage1);
        messageRepository.saveMessage(null, sequencedMessage2);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        List<SequencedMessage> result = messageRepository.getWaitingAndProcessedMessagesInNewTransaction(sequenceInstance);

        assertThat(result).contains(sequencedMessage1, sequencedMessage2);
    }

    @Test
    void getBufferedMessageInNewTransaction_returnsCorrectMessage() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstance.getId())
                .messageType("type")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .build();
        BufferedMessage bufferedMessage = BufferedMessage.builder()
                .sequenceInstanceId(1L)
                .key(new byte[]{1, 2, 3})
                .value(new byte[]{4, 5, 6})
                .sequenceInstanceId(sequenceInstance.getId())
                .build();
        messageRepository.saveMessage(bufferedMessage, sequencedMessage);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        BufferedMessage result = messageRepository.getBufferedMessageInNewTransaction(sequencedMessage);

        assertThat(result).isEqualTo(bufferedMessage);
    }

    @Test
    void setMessageStateInNewTransaction_updatesStateCorrectly() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(1L)
                .messageType("type")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .sequenceInstanceId(sequenceInstance.getId())
                .build();
        testEntityManager.persist(sequencedMessage);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        messageRepository.setMessageStateInNewTransaction(sequencedMessage, SequencedMessageState.PROCESSED);

        TestTransaction.start();
        SequencedMessage updatedMessage = testEntityManager.find(SequencedMessage.class, sequencedMessage.getId());
        assertThat(updatedMessage.getState()).isEqualTo(SequencedMessageState.PROCESSED);
    }

    @Test
    void setMessageStateInCurrentTransaction_updatesStateCorrectly() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(1L)
                .messageType("type")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .sequenceInstanceId(sequenceInstance.getId())
                .build();
        messageRepository.saveMessage(null, sequencedMessage);

        messageRepository.setMessageStateInCurrentTransaction(sequencedMessage, SequencedMessageState.PROCESSED);

        testEntityManager.clear();
        SequencedMessage updatedMessage = testEntityManager.find(SequencedMessage.class, sequencedMessage.getId());
        assertThat(updatedMessage.getState()).isEqualTo(SequencedMessageState.PROCESSED);
    }

    @Test
    void findByMessageTypeAndIdempotenceId_returnsCorrectMessageInNewTransaction() {
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(1L)
                .messageType("type")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .sequenceInstanceId(sequenceInstance.getId())
                .build();
        messageRepository.saveMessage(null, sequencedMessage);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Optional<SequencedMessage> result = messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("type", "idempotenceId");

        assertThat(result)
                .isPresent()
                .contains(sequencedMessage);
    }

    private SequenceInstance createAndPersistSequenceInstance() {
        return sequenceInstanceRepository.save(SequenceInstance.builder()
                .name("test")
                .contextId(UUID.randomUUID().toString())
                .state(SequenceInstanceState.OPEN)
                .retentionPeriod(Duration.ofDays(7))
                .build());
    }

    @Test
    void deleteExpiredMessages_deletesOnlyExpiredMessages() {
        // Given
        SequenceInstance sequenceInstance = createAndPersistSequenceInstance();

        // Create a sequence instance that has already expired (retain_until is in the past)
        SequenceInstance expiredSequenceInstance = sequenceInstanceRepository.save(SequenceInstance.builder()
                .name("test-expired")
                .contextId(UUID.randomUUID().toString())
                .state(SequenceInstanceState.OPEN)
                .retentionPeriod(Duration.ofDays(7))
                .build());

        // Set retainUntil to a time in the past for the sequence instance
        try {
            Field field = SequenceInstance.class.getDeclaredField("retainUntil");
            field.setAccessible(true);
            field.set(expiredSequenceInstance, ZonedDateTime.now().minusHours(1));
        } catch (Exception e) {
            throw new RuntimeException("Could not set retainUntil field", e);
        }

        // Create a message that belongs to the expired sequence
        SequencedMessage expiredMessage = SequencedMessage.builder()
                .messageType("TestMessage")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("test-idempotence-id-1")
                .sequenceInstanceId(expiredSequenceInstance.getId())
                .state(SequencedMessageState.PROCESSED)
                .clusterName("test-cluster")
                .topic("test-topic")
                .build();

        // Create a message that has not expired yet (in a sequence where retain_until is in the future)
        SequencedMessage validMessage = SequencedMessage.builder()
                .messageType("TestMessage")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("test-idempotence-id-2")
                .sequenceInstanceId(sequenceInstance.getId())
                .state(SequencedMessageState.PROCESSED)
                .clusterName("test-cluster")
                .topic("test-topic")
                .build();

        // Create buffered messages for both sequenced messages
        BufferedMessage expiredBufferedMessage = BufferedMessage.builder()
                .key(new byte[]{1, 2, 3})
                .value(new byte[]{4, 5, 6})
                .sequenceInstanceId(expiredSequenceInstance.getId())
                .build();

        BufferedMessage validBufferedMessage = BufferedMessage.builder()
                .key(new byte[]{7, 8, 9})
                .value(new byte[]{10, 11, 12})
                .sequenceInstanceId(sequenceInstance.getId())
                .build();

        messageRepository.saveMessage(expiredBufferedMessage, expiredMessage);
        messageRepository.saveMessage(validBufferedMessage, validMessage);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // When
        TestTransaction.start();
        ZonedDateTime now = ZonedDateTime.now();
        int deletedCount = messageRepository.deleteExpiredMessages(now);

        // Then
        assertThat(deletedCount).isEqualTo(1);

        // Verify only the expired sequenced message was deleted
        assertThat(sequencedMessageRepository.findById(validMessage.getId())).isPresent();
        assertThat(sequencedMessageRepository.findById(expiredMessage.getId())).isEmpty();

        // Verify only the expired buffered message was deleted
        assertThat(testEntityManager.find(BufferedMessage.class, validBufferedMessage.getId())).isNotNull();
        assertThat(testEntityManager.find(BufferedMessage.class, expiredBufferedMessage.getId())).isNull();
    }
}
