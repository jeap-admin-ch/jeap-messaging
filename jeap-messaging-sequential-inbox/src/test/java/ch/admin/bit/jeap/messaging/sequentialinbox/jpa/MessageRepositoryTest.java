package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = MessageRepositoryTest.TestConfig.class)
class MessageRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

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
        long sequenceInstanceId = createAndPersistSequenceInstance();
        BufferedMessage bufferedMessage = BufferedMessage.builder()
                .sequenceInstanceId(1L)
                .key(new byte[]{1, 2, 3})
                .value(new byte[]{4, 5, 6})
                .sequenceInstanceId(sequenceInstanceId)
                .build();
        SequencedMessage sequencedMessage = createSequencedMessage(sequenceInstanceId);

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
        long sequenceInstanceId = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = createSequencedMessage(sequenceInstanceId);

        messageRepository.saveMessage(null, sequencedMessage);

        assertThat(testEntityManager.find(SequencedMessage.class, sequencedMessage.getId()))
                .isNotNull();
    }

    @Test
    void getProcessedMessageTypesInSequenceInNewTransaction_returnsCorrectTypes() {
        long sequenceInstanceId = createAndPersistSequenceInstance();

        SequencedMessage sequencedMessage1 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstanceId)
                .messageType("type1")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId1")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.PROCESSED)
                .build();
        SequencedMessage sequencedMessage2 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstanceId)
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

        Set<String> result = messageRepository.getProcessedMessageTypesInSequenceInNewTransaction(sequenceInstanceId);

        assertThat(result).contains("type1", "type2");
    }

    @Test
    void getWaitingAndProcessedMessagesInNewTransaction_returnsCorrectMessages() {
        long sequenceInstanceId = createAndPersistSequenceInstance();

        SequencedMessage sequencedMessage1 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstanceId)
                .messageType("type1")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId1")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .build();
        SequencedMessage sequencedMessage2 = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstanceId)
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

        List<SequencedMessage> result = messageRepository.getWaitingAndProcessedMessagesInNewTransaction(sequenceInstanceId);

        assertThat(result).contains(sequencedMessage1, sequencedMessage2);
    }

    @Test
    void getBufferedMessageInNewTransaction_returnsCorrectMessage() {
        long sequenceInstanceId = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstanceId)
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
                .sequenceInstanceId(sequenceInstanceId)
                .build();
        messageRepository.saveMessage(bufferedMessage, sequencedMessage);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        BufferedMessage result = messageRepository.getBufferedMessageInNewTransaction(sequencedMessage);

        assertThat(result).isEqualTo(bufferedMessage);
    }

    @Test
    void setMessageStateInNewTransaction_updatesStateCorrectly() {
        long sequenceInstanceId = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = createSequencedMessage(sequenceInstanceId);
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
        long sequenceInstanceId = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = createSequencedMessage(sequenceInstanceId);
        messageRepository.saveMessage(null, sequencedMessage);

        messageRepository.setMessageStateInCurrentTransaction(sequencedMessage, SequencedMessageState.PROCESSED);

        testEntityManager.clear();
        SequencedMessage updatedMessage = testEntityManager.find(SequencedMessage.class, sequencedMessage.getId());
        assertThat(updatedMessage.getState()).isEqualTo(SequencedMessageState.PROCESSED);
    }

    @Test
    void findByMessageTypeAndIdempotenceId_returnsCorrectMessageInNewTransaction() {
        long sequenceInstanceId = createAndPersistSequenceInstance();
        SequencedMessage sequencedMessage = createSequencedMessage(sequenceInstanceId);
        messageRepository.saveMessage(null, sequencedMessage);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Optional<SequencedMessage> result = messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("type", "idempotenceId");

        assertThat(result)
                .isPresent()
                .contains(sequencedMessage);
    }

    @Test
    void deleteExpiredMessages_deletesOnlyExpiredMessages() {
        // Given
        long sequenceInstanceId = createAndPersistSequenceInstance();

        // Create a sequence instance that has already expired (retain_until is in the past)
        String contextId = UUID.randomUUID().toString();
        String name = "test-expired";
        long expiredSequenceInstanceId = sequenceInstanceRepository.saveNewInstance(SequenceInstance.builder()
                .name(name)
                .contextId(contextId)
                .state(SequenceInstanceState.OPEN)
                .retentionPeriod(Duration.ofDays(7))
                .build());


        // Expire one sequence instance
        testEntityManager.getEntityManager()
                .createNativeQuery("UPDATE sequence_instance SET retain_until='2007-01-01' WHERE id=" + expiredSequenceInstanceId)
                .executeUpdate();

        // Create a message that belongs to the expired sequence
        SequencedMessage expiredMessage = SequencedMessage.builder()
                .messageType("TestMessage")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("test-idempotence-id-1")
                .sequenceInstanceId(expiredSequenceInstanceId)
                .state(SequencedMessageState.PROCESSED)
                .clusterName("test-cluster")
                .topic("test-topic")
                .build();

        // Create a message that has not expired yet (in a sequence where retain_until is in the future)
        SequencedMessage validMessage = SequencedMessage.builder()
                .messageType("TestMessage")
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("test-idempotence-id-2")
                .sequenceInstanceId(sequenceInstanceId)
                .state(SequencedMessageState.PROCESSED)
                .clusterName("test-cluster")
                .topic("test-topic")
                .build();

        // Create buffered messages for both sequenced messages
        BufferedMessage expiredBufferedMessage = BufferedMessage.builder()
                .key(new byte[]{1, 2, 3})
                .value(new byte[]{4, 5, 6})
                .sequenceInstanceId(expiredSequenceInstanceId)
                .build();

        BufferedMessage validBufferedMessage = BufferedMessage.builder()
                .key(new byte[]{7, 8, 9})
                .value(new byte[]{10, 11, 12})
                .sequenceInstanceId(sequenceInstanceId)
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


    @Test
    void getWaitingMessageCountByType() {
        long sequenceInstanceId = createAndPersistSequenceInstance();
        SequencedMessage waitingMessage = createSequencedMessage(sequenceInstanceId);
        SequencedMessage processedMessage = createSequencedMessage(sequenceInstanceId);
        SequencedMessage otherTypeWaitingMessage = createSequencedMessage(sequenceInstanceId, "otherType");
        processedMessage.setState(SequencedMessageState.PROCESSED);
        messageRepository.saveMessage(null, waitingMessage);
        messageRepository.saveMessage(null, otherTypeWaitingMessage);
        messageRepository.saveMessage(null, processedMessage);

        Map<String, Double> countByType = messageRepository.getWaitingMessageCountByType();

        assertThat(countByType)
                .containsEntry("type", 1.0)
                .containsEntry("otherType", 1.0)
                .hasSize(2);

    }

    private static SequencedMessage createSequencedMessage(long instanceId) {
        return createSequencedMessage(instanceId, "type");
    }

    private static SequencedMessage createSequencedMessage(long instanceId, String type) {
        return SequencedMessage.builder()
                .sequenceInstanceId(instanceId)
                .messageType(type)
                .sequencedMessageId(UUID.randomUUID())
                .idempotenceId("idempotenceId")
                .clusterName("cluster")
                .topic("topic")
                .state(SequencedMessageState.WAITING)
                .build();
    }

    private long createAndPersistSequenceInstance() {
        return sequenceInstanceRepository.saveNewInstance(SequenceInstance.builder()
                .name("test")
                .contextId(UUID.randomUUID().toString())
                .state(SequenceInstanceState.OPEN)
                .retentionPeriod(Duration.ofDays(7))
                .build());
    }
}
