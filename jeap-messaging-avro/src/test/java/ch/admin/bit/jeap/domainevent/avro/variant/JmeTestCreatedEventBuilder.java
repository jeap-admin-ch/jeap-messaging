package ch.admin.bit.jeap.domainevent.avro.variant;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jme.messaging.event.test.created.JmeTestCreatedEvent;
import lombok.Getter;

@Getter
public class JmeTestCreatedEventBuilder extends AvroDomainEventBuilder<JmeTestCreatedEventBuilder, JmeTestCreatedEvent> {
    private final String systemName = "JME";
    private final String eventName = "JmeTestCreatedEvent";
    private String serviceName = "jeap-microservice-examples-kafka";

    private String message;
    private JmeTestCreatedEventBuilder() {
        super(JmeTestCreatedEvent::new);
    }

    public static JmeTestCreatedEventBuilder create() {
        return new JmeTestCreatedEventBuilder();
    }

    public JmeTestCreatedEventBuilder message(String message) {
        this.message = message;
        return this;
    }

    public JmeTestCreatedEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    protected JmeTestCreatedEventBuilder self() {
        return this;
    }

    @Override
    public JmeTestCreatedEvent build() {
        return super.build();
    }
}
