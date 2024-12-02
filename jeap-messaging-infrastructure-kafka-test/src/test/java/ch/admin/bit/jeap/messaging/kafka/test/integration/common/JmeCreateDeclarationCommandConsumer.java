package ch.admin.bit.jeap.messaging.kafka.test.integration.common;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.kafka.test.TestKafkaListener;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JmeCreateDeclarationCommandConsumer {

    public static final String TOPIC_NAME = JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC;

    @Autowired(required = false)
    private final List<MessageListener<JmeCreateDeclarationCommand>> jmeCreateDeclarationCommandProcessors;

    @TestKafkaListener(topics = TOPIC_NAME)
    public void consume(final JmeCreateDeclarationCommand command, Acknowledgment ack) {
        jmeCreateDeclarationCommandProcessors.forEach(commandProcessor -> commandProcessor.receive(command));
        ack.acknowledge();
    }
}
