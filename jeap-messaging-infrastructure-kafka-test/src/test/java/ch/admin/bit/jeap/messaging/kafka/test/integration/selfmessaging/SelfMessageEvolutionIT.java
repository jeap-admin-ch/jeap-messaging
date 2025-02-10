package ch.admin.bit.jeap.messaging.kafka.test.integration.selfmessaging;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.ContractsValidationConfiguration;
import ch.admin.bit.jme.test.BeanReference;
import ch.admin.bit.jme.test.JmeBackwardSchemaEvolutionTestEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * This test shows the intermediate step of a self-messaging schema evolution, by sending an event with V1 schema
 * and receiving it with a V2 (backwards compatible)
 * <p>
 * Initial state
 * App [Consumer Schema V1, Producer Schema V1]
 * <p>
 * First deployment (rolling deployment)
 * App [Consumer Schema V1, Producer Schema V1]
 * App' [Consumer Schema V2, Producer Schema V1] <-- This state
 * <p>
 * State after rolling deployment:
 * App' [Consumer Schema V2, Producer Schema V1] <-- This state
 */

@SpringBootTest(classes = {SelfMessagingTestConfig.class, ContractsValidationConfiguration.class}, properties = {"spring.application.name=jme-self-messaging-service"})
@SuppressWarnings("java:S2699") // asserts are in the super class methods, but sonar does not get it
@DirtiesContext
class SelfMessageEvolutionIT extends KafkaIntegrationTestBase {

    @MockitoBean
    private MessageListener<ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent> testV2EventProcessors;

    @Test
    void testConsumeEventWithContract() {
        BeanReference beanReference = BeanReference.newBuilder().setId("id").setName("name").setNamespace("namespace").setType("type").build();
        // Send a V1 Event
        JmeBackwardSchemaEvolutionTestEvent backwardSchemaEvolutionTestEvent = JmeBackwardSchemaEvolutionTestEventBuilder.create()
                .idempotenceId("idempotenceId")
                .message("someMessage")

                .beanReference(beanReference).build();
        sendSync("jme-backward-schema-evolution-test-event", backwardSchemaEvolutionTestEvent);

        /**
         * Receive as a V2 Event
         * @see SelfMessagingTestConfig#consume(ch.admin.bit.jme.test.v2.JmeBackwardSchemaEvolutionTestEvent, Acknowledgment)
         */
        Mockito.verify(testV2EventProcessors, Mockito.timeout(TEST_TIMEOUT)).receive(Mockito.any());
    }
}