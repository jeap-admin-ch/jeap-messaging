package ch.admin.bit.jeap.messaging.idempotence.testsupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SleepyHandlersConfig {

    @Bean
    public SleepyTestMessageHandler quickSleepyTestMessageHandler() {
        return new SleepyTestMessageHandler(Duration.ofMillis(500));
    }

    @Bean
    public SleepyTestMessageHandler slowSleepyTestMessageHandler() {
        return new SleepyTestMessageHandler(Duration.ofSeconds(1));
    }

}
