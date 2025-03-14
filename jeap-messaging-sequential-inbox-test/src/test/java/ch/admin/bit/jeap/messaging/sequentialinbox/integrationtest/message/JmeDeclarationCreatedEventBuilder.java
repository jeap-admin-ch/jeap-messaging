package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jme.declaration.DeclarationPayload;
import ch.admin.bit.jme.declaration.DeclarationReferences;
import ch.admin.bit.jme.declaration.JmeDeclarationCreatedEvent;
import lombok.Getter;

@Getter
public class JmeDeclarationCreatedEventBuilder extends AvroDomainEventBuilder<JmeDeclarationCreatedEventBuilder, JmeDeclarationCreatedEvent> {
    private final String specifiedMessageTypeVersion = "1.0.0";
    private String serviceName = "test";
    private final String systemName = "test";
    private String message;
    private String processId;

    private JmeDeclarationCreatedEventBuilder() {
        super(JmeDeclarationCreatedEvent::new);
    }

    public static JmeDeclarationCreatedEventBuilder create() {
        return new JmeDeclarationCreatedEventBuilder();
    }

    public JmeDeclarationCreatedEventBuilder message(String message) {
        this.message = message;
        return self();
    }

    public JmeDeclarationCreatedEventBuilder processId(String processId) {
        this.processId = processId;
        return self();
    }

    public JmeDeclarationCreatedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return self();
    }

    @Override
    protected JmeDeclarationCreatedEventBuilder self() {
        return this;
    }

    @Override
    public JmeDeclarationCreatedEvent build() {
        setReferences(DeclarationReferences.newBuilder().build());
        setPayload(DeclarationPayload.newBuilder().setMessage(message).build());
        setProcessId(processId);
        return super.build();
    }
}
