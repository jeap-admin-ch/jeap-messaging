package ch.admin.bit.jeap.domainevent.avro.variant;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.declaration.DeclarationPayload;
import ch.admin.bit.jme.declaration.DeclarationReferences;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.Getter;

@Getter
public class JmeDeclarationCreatedEventBuilder extends AvroDomainEventBuilder<JmeDeclarationCreatedEventBuilder, JmeDeclarationCreatedEvent> {
    private final String systemName = "JME";
    private final String eventName = "JmeDeclarationCreatedEvent";
    private String serviceName = "jeap-microservice-examples-kafka";

    private String message;
    private JmeDeclarationCreatedEventBuilder() {
        super(JmeDeclarationCreatedEvent::new);
    }

    public static JmeDeclarationCreatedEventBuilder create() {
        return new JmeDeclarationCreatedEventBuilder();
    }

    public JmeDeclarationCreatedEventBuilder message(String message) {
        this.message = message;
        return this;
    }

    public JmeDeclarationCreatedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    protected JmeDeclarationCreatedEventBuilder self() {
        return this;
    }

    @Override
    public JmeDeclarationCreatedEvent build() {
        if (this.message == null) {
            throw AvroMessageBuilderException.propertyNull("jmeDeclarationCreatedEventReferences.message");
        }
        DeclarationReferences declarationReferences = DeclarationReferences.newBuilder()
                .build();
        DeclarationPayload declarationPayload = DeclarationPayload.newBuilder()
                .setMessage(message)
                .build();
        setReferences(declarationReferences);
        setPayload(declarationPayload);
        return super.build();
    }
}
