package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TestCommandConsumer {
    public static final String TOPIC_NAME = "test-command-topic";

    @Autowired(required = false)
    private final List<MessageListener<JmeCreateDeclarationCommand>> testCommandProcessors;

    @TestKafkaListener(topics = TOPIC_NAME)
    public void consume(final JmeCreateDeclarationCommand command, Acknowledgment ack) {
        testCommandProcessors.forEach(testEventProcessor -> testEventProcessor.receive(command));
        ack.acknowledge();
    }
}
