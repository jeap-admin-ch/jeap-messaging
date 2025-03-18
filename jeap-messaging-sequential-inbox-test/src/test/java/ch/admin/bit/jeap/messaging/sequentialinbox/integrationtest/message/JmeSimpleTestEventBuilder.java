package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEventPayload;
import ch.admin.bit.jme.test.JmeSimpleTestEventReferences;
import lombok.Getter;

@Getter
public class JmeSimpleTestEventBuilder extends AvroDomainEventBuilder<JmeSimpleTestEventBuilder, JmeSimpleTestEvent> {
    private final String specifiedMessageTypeVersion = "1.0.0";
    private String serviceName = "test";
    private final String systemName = "test";
    private String message;
    private String processId;

    private JmeSimpleTestEventBuilder() {
        super(JmeSimpleTestEvent::new);
    }

    public static JmeSimpleTestEventBuilder create() {
        return new JmeSimpleTestEventBuilder();
    }

    public JmeSimpleTestEventBuilder message(String message) {
        this.message = message;
        return self();
    }

    public JmeSimpleTestEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return self();
    }

    public JmeSimpleTestEventBuilder processId(String processId) {
        this.processId = processId;
        return self();
    }

    @Override
    protected JmeSimpleTestEventBuilder self() {
        return this;
    }

    @Override
    public JmeSimpleTestEvent build() {
        setReferences(JmeSimpleTestEventReferences.newBuilder().build());
        setPayload(JmeSimpleTestEventPayload.newBuilder().setMessage(message).build());
        setProcessId(processId);
        return super.build();
    }
}
