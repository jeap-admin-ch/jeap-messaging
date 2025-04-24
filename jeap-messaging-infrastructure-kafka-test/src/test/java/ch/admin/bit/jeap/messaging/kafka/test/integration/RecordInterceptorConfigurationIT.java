package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeDeclarationCreatedEventConsumer;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.TestConfig;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestConfig.class, properties = {
        "spring.application.name=jme-messaging-subscriber-service"
})
@Slf4j
@DirtiesContext
@Import(RecordInterceptorConfigurationIT.TestConfig.class)
class RecordInterceptorConfigurationIT extends KafkaIntegrationTestBase {
    @MockitoBean
    private MessageListener<JmeDeclarationCreatedEvent> jmeEventProcessor;

    static class TestConfig {
        static boolean interceptor1Called = false;
        static boolean interceptor2Called = false;

        @Bean
        RecordInterceptor<Object, Object> recordInterceptor1() {
            return (record, consumer) -> {
                log.info("First interceptor intercepted record: {}", record);
                interceptor1Called = true;
                return record;
            };
        }

        @Bean
        RecordInterceptor<Object, Object> recordInterceptor2() {
            return (record, consumer) -> {
                log.info("Second interceptor intercepted record: {}", record);
                interceptor2Called = true;
                return record;
            };
        }
    }

    @Test
    void customInterceptorsAreInvoked() {
        sendSync(JmeDeclarationCreatedEventConsumer.TOPIC_NAME, JmeDeclarationCreatedEventBuilder.create()
                .idempotenceId("idempotenceId").message("customConfig").build());
        verify(jmeEventProcessor, timeout(TEST_TIMEOUT)).receive(any());

        assertThat(TestConfig.interceptor1Called)
                .isTrue();
        assertThat(TestConfig.interceptor2Called)
                .isTrue();
    }
}
