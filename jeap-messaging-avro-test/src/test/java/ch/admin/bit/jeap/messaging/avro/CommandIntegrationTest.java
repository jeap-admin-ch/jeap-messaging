package ch.admin.bit.jeap.messaging.avro;


import ch.admin.bit.jeap.command.avro.AvroCommand;
import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.messaging.avro.command.integration.idl.IdlTestIntegrationCommand;
import ch.admin.bit.jeap.messaging.avro.command.integration.idl.IdlTestIntegrationReference;
import ch.admin.bit.jeap.messaging.avro.command.integration.idl.IdlTestIntegrationReferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandIntegrationTest {

    @Test
    void testGeneratedEvent() {
        final IdlTestIntegrationCommand command = new IdlTestIntegrationCommandBuilder()
                .systemId("systemId")
                .idempotenceId("idempotenceId")
                .build();
        //noinspection ConstantConditions
        assertTrue(command instanceof AvroCommand); // Checks that correct interface is set during code generation
    }

    static class IdlTestIntegrationCommandBuilder extends AvroCommandBuilder<IdlTestIntegrationCommandBuilder, IdlTestIntegrationCommand> {
        private String systemId;

        IdlTestIntegrationCommandBuilder() {
            super(IdlTestIntegrationCommand::new);
        }

        @SuppressWarnings("SameParameterValue")
        IdlTestIntegrationCommandBuilder systemId(final String systemId) {
            this.systemId = systemId;
            return self();
        }

        @Override
        public IdlTestIntegrationCommand build() {
            if (systemId == null) {
                throw AvroMessageBuilderException.propertyNull("references.systemId");
            }
            IdlTestIntegrationReference system = IdlTestIntegrationReference.newBuilder()
                    .setType("system").setId("id").build();
            setReferences(new IdlTestIntegrationReferences(system));
            return super.build();
        }

        @Override
        protected String getServiceName() {
            return "testService";
        }

        @Override
        protected String getSystemName() {
            return "testSystem";
        }

        @Override
        protected String getSpecifiedMessageTypeVersion() {
            return "testVersion";
        }

        @Override
        protected IdlTestIntegrationCommandBuilder self() {
            return this;
        }
    }
}
