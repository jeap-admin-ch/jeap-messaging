package ch.admin.bit.jeap.messaging.kafka.test.integration.selfmessaging;


import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.test.BeanReference;
import ch.admin.bit.jme.test.JmeBackwardSchemaEvolutionTestEvent;
import ch.admin.bit.jme.test.JmeBackwardSchemaEvolutionTestEventPayload;
import ch.admin.bit.jme.test.JmeBackwardSchemaEvolutionTestEventReferences;

public class JmeBackwardSchemaEvolutionTestEventBuilder extends AvroDomainEventBuilder<JmeBackwardSchemaEvolutionTestEventBuilder, JmeBackwardSchemaEvolutionTestEvent> {
    private String message;
    private BeanReference beanReference;

    private JmeBackwardSchemaEvolutionTestEventBuilder() {
        super(JmeBackwardSchemaEvolutionTestEvent::new);
    }

    public static JmeBackwardSchemaEvolutionTestEventBuilder create() {
        return new JmeBackwardSchemaEvolutionTestEventBuilder();
    }

    public JmeBackwardSchemaEvolutionTestEventBuilder message(String message) {
        this.message = message;
        return this;
    }

    public JmeBackwardSchemaEvolutionTestEventBuilder beanReference(BeanReference beanReference) {
        this.beanReference = beanReference;
        return this;
    }

    @Override
    protected String getServiceName() {
        return "jme-self-messaging-service";
    }

    @Override
    protected String getSystemName() {
        return "JME";
    }

    @Override
    protected JmeBackwardSchemaEvolutionTestEventBuilder self() {
        return this;
    }

    @Override
    public JmeBackwardSchemaEvolutionTestEvent build() {
        if (this.message == null) {
            throw AvroMessageBuilderException.propertyNull("jmeBackwardSchemaEvolutionTestEventPayload.message");
        }
        if (this.beanReference == null) {
            throw AvroMessageBuilderException.propertyNull("jmeBackwardSchemaEvolutionTestEventPayload.beanReference");
        }
        JmeBackwardSchemaEvolutionTestEventReferences references = JmeBackwardSchemaEvolutionTestEventReferences.newBuilder()
                .setSendingBean(beanReference)
                .build();
        JmeBackwardSchemaEvolutionTestEventPayload payload = JmeBackwardSchemaEvolutionTestEventPayload.newBuilder()
                .setMessage(message)
                .build();
        setReferences(references);
        setPayload(payload);
        return super.build();
    }
}
