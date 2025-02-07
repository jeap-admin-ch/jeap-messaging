package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.sequentialinbox.test.TestEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext
@AutoConfigureObservability
@SpringBootTest
@Slf4j
class SequentialInboxIT extends KafkaIntegrationTestBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    SequenceInstanceRepository sequenceInstanceRepository;

    @MockitoBean
    private ContractsValidator contractsValidator; // Disable contract checking by mocking the contracts validator

    @Autowired
    private TestEventListener testEventListener;

    @Transactional
    @Test
    void testInbox() {

        assertThat(sequenceInstanceRepository.findAll()).isEmpty();

        final String idempotenceIdEvent = "idempotenceId1";
        final TestEvent testEvent = TestEventBuilder.create().idempotenceId(idempotenceIdEvent).build();

        sendSync("test-topic-1", testEvent);

//        await().atMost(Duration.ofSeconds(60)).until(() -> testEventListener.getEvents().size() == 1);
//
//        log.info("Message received: {}", testEventListener.getEvents().getFirst());


    }


}
