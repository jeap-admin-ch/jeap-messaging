package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.sequentialinbox.housekeeping.SequentialInboxHousekeepingService;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.MessageRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.jpa.SequenceInstanceRepository;
import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class SequentialInboxHousekeepingServiceIT extends SequentialInboxITBase {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SequenceInstanceRepository sequenceInstanceRepository;

    @Autowired
    private SequentialInboxHousekeepingService housekeepingService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void housekeeping_closedSequence_should_be_deleted() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        String idempotenceIdMessage1 = UUID.randomUUID().toString();
        String idempotenceIdMessage2 = UUID.randomUUID().toString();

        transactionTemplate.executeWithoutResult(status -> {
            long sequenceInstanceId = saveSequenceInstance(UUID.randomUUID().toString(), SequenceInstanceState.CLOSED);
            saveMessage("testMessageType", sequenceInstanceId, idempotenceIdMessage1, true);

            long sequenceInstance2Id = saveSequenceInstance(UUID.randomUUID().toString(), SequenceInstanceState.OPEN);
            saveMessage("testMessageType", sequenceInstance2Id, idempotenceIdMessage2, true);
        });

        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage1)).isPresent();

        housekeepingService.deleteClosedSequenceInstances();

        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage1)).isEmpty();
        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage2)).isPresent();
    }

    @Test
    void housekeeping_expiredSequence_should_be_deleted() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        String idempotenceIdMessage1 = UUID.randomUUID().toString();
        String idempotenceIdMessage2 = UUID.randomUUID().toString();

        transactionTemplate.executeWithoutResult(status -> {
            long sequenceInstanceId = saveSequenceInstance(UUID.randomUUID().toString(), SequenceInstanceState.OPEN);
            saveMessage("testMessageType", sequenceInstanceId, idempotenceIdMessage1, true);
            saveMessage("testMessageType", sequenceInstanceId, idempotenceIdMessage2, false);
        });

        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage1)).isPresent();
        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage2)).isPresent();

        housekeepingService.deleteExpiredMessages();

        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage1)).isEmpty();
        assertThat(messageRepository.findByMessageTypeAndIdempotenceIdInNewTransaction("testMessageType", idempotenceIdMessage2)).isEmpty();
    }

    private long saveSequenceInstance(String contextId, SequenceInstanceState state) {
        SequenceInstance sequenceInstance = SequenceInstance.builder()
                .contextId(contextId)
                .state(state)
                .name("test")
                .retentionPeriod(Duration.ofNanos(1))
                .build();
        return sequenceInstanceRepository.saveNewInstance(sequenceInstance);
    }

    private void saveMessage(String messageType, long sequenceInstanceId, String idempotenceId, boolean withHeaders) {
        BufferedMessage bufferedMessage = BufferedMessage.builder()
                .key("testKey".getBytes())
                .value("testValue".getBytes())
                .sequenceInstanceId(sequenceInstanceId)
                .build();

        if (withHeaders) {
            List<MessageHeader> headers = List.of(MessageHeader.builder()
                    .headerName("testHeader")
                    .headerValue("test".getBytes())
                    .bufferedMessage(bufferedMessage)
                    .build());
            bufferedMessage.setHeaders(headers);
        }

        SequencedMessage sequencedMessage = SequencedMessage.builder()
                .sequenceInstanceId(sequenceInstanceId)
                .sequencedMessageId(UUID.randomUUID())
                .messageType(messageType)
                .state(SequencedMessageState.WAITING)
                .idempotenceId(idempotenceId)
                .clusterName("testCluster")
                .topic("testTopic")
                .build();

        messageRepository.saveMessage(bufferedMessage, sequencedMessage);
    }

}
