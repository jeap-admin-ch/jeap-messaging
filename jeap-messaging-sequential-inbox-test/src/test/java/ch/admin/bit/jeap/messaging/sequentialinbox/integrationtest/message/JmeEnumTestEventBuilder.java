package ch.admin.bit.jeap.messaging.sequentialinbox.integrationtest.message;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jme.test.JmeEnumTestEvent;
import ch.admin.bit.jme.test.JmeEnumTestEventPayload;
import ch.admin.bit.jme.test.SomeEnum;
import lombok.Getter;

@Getter
public class JmeEnumTestEventBuilder extends AvroDomainEventBuilder<JmeEnumTestEventBuilder, JmeEnumTestEvent> {
    private final String specifiedMessageTypeVersion = "1.0.0";
    private final String serviceName = "test";
    private final String systemName = "test";
    private String message;
    private String processId;

    private JmeEnumTestEventBuilder() {
        super(JmeEnumTestEvent::new);
    }

    public static JmeEnumTestEventBuilder create() {
        return new JmeEnumTestEventBuilder();
    }

    public JmeEnumTestEventBuilder message(String message) {
        this.message = message;
        return self();
    }

    public JmeEnumTestEventBuilder processId(String processId) {
        this.processId = processId;
        return self();
    }

    @Override
    protected JmeEnumTestEventBuilder self() {
        return this;
    }

    @Override
    public JmeEnumTestEvent build() {
        setPayload(JmeEnumTestEventPayload.newBuilder()
                .setMessage(message)
                .setMyEnum(SomeEnum.A)
                .build());
        setProcessId(processId);
        return super.build();
    }
}
