package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContracts;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@JeapMessageConsumerContracts({
        JmeDeclarationCreatedEvent.TypeRef.class,
        JmeSimpleTestEvent.TypeRef.class,
        JmeEnumTestEvent.TypeRef.class})
public class TestApp {
}
