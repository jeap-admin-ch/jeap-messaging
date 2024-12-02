package ch.admin.bit.jeap.messaging.avro;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEvent;
import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jeap.domainevent.avro.event.integration.idl.IdlTestIntegrationEvent;
import ch.admin.bit.jeap.domainevent.avro.event.integration.idl.IdlTestIntegrationReference;
import ch.admin.bit.jeap.domainevent.avro.event.integration.idl.IdlTestIntegrationReferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventIntegrationTest {

    @Test
    void testGeneratedEvent() {
        final IdlTestIntegrationEvent event = new IdlTestIntegrationEventBuilder()
                .systemId("systemId")
                .idempotenceId("idempotenceId")
                .build();
        //noinspection ConstantConditions
        assertTrue(event instanceof AvroDomainEvent); // Checks that correct interface is set during code generation
    }

    static class IdlTestIntegrationEventBuilder extends AvroDomainEventBuilder<IdlTestIntegrationEventBuilder, IdlTestIntegrationEvent> {
        private String systemId;

        IdlTestIntegrationEventBuilder() {
            super(IdlTestIntegrationEvent::new);
        }

        @SuppressWarnings("SameParameterValue")
        IdlTestIntegrationEventBuilder systemId(final String systemId) {
            this.systemId = systemId;
            return self();
        }

        @Override
        public IdlTestIntegrationEvent build() {
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
        protected IdlTestIntegrationEventBuilder self() {
            return this;
        }
    }
}
