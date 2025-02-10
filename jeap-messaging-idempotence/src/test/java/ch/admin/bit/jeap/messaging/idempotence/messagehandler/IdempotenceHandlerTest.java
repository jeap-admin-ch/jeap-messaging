package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = {IdempotentMessageHandlerConfig.class},
        properties = {"jeap.messaging.idempotent-message-handler.advice-order=42"})
public class IdempotenceHandlerTest {

    @Autowired
    IdempotentMessageHandlerAspect idempotentMessageHandlerAspect;

    @MockitoBean
    IdempotentProcessingRepository idempotentProcessingRepository;

    @Test
    void testIdempotentMessageHandlerAspectCustomAdviceOrder() {
        assertThat(idempotentMessageHandlerAspect.getOrder()).isEqualTo(42);
    }

}
