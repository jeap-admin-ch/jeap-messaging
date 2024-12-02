package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;


import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;
import ch.admin.bit.jme.declaration.CreateDeclarationPayload;
import ch.admin.bit.jme.declaration.CreateDeclarationReferences;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;

public class JmeCreateDeclarationCommandBuilder extends AvroCommandBuilder<JmeCreateDeclarationCommandBuilder, JmeCreateDeclarationCommand> {

    private String text;

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

    @Override
    protected String getServiceName() {
        return "jeap-microservice-examples-kafka";
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
