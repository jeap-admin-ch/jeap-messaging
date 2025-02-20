package ch.admin.bit.jeap.messaging.kafka.test.integration.common;


import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.declaration.*;

public class JmeCreateDeclarationCommandBuilder extends AvroCommandBuilder<JmeCreateDeclarationCommandBuilder, JmeCreateDeclarationCommand> {

    private String text;
    private String serviceName = "jeap-microservice-examples-kafka";

    private JmeCreateDeclarationCommandBuilder() {
        super(JmeCreateDeclarationCommand::new);
    }

    public static JmeCreateDeclarationCommandBuilder create() {
        return new JmeCreateDeclarationCommandBuilder();
    }

    public JmeCreateDeclarationCommandBuilder text(String text) {
        this.text = text;
        return this;
    }

    public JmeCreateDeclarationCommandBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @Override
    protected String getServiceName() {
        return serviceName;
    }

    @Override
    protected String getSystemName() {
        return "JME";
    }

    @Override
    protected JmeCreateDeclarationCommandBuilder self() {
        return this;
    }

    @Override
    public JmeCreateDeclarationCommand build() {
        if (this.text == null) {
            throw AvroMessageBuilderException.propertyNull("text");
        }
        CreateDeclarationReferences createDeclarationReferences = CreateDeclarationReferences.newBuilder()
                .build();
        CreateDeclarationPayload createDeclarationPayload = CreateDeclarationPayload.newBuilder()
                .setText(text)
                .build();
        setReferences(createDeclarationReferences);
        setPayload(createDeclarationPayload);
        return super.build();
    }
}
