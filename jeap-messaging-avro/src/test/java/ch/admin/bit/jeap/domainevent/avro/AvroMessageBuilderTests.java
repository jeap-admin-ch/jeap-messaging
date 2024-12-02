package ch.admin.bit.jeap.domainevent.avro;

import ch.admin.bit.jeap.command.avro.AvroCommand;
import ch.admin.bit.jeap.command.avro.AvroCommandBuilder;
import ch.admin.bit.jeap.domainevent.DomainEvent;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageIdentity;
import ch.admin.bit.jeap.messaging.avro.AvroMessagePublisher;
import ch.admin.bit.jeap.messaging.avro.AvroMessageType;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import lombok.Data;
import lombok.Getter;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AvroMessageBuilderTests {
    @Test
    void create() {
        SimpleEvent simpleEvent = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(simpleEvent);
        assertNotNull(simpleEvent.getReferences());
    }

    @Test
    void domainEventVersion() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertEquals("1.2.0", target.getDomainEventVersion());
    }

    @Test
    void identity() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getIdentity());
        assertNotNull(target.getIdentity().getId());
        assertNotNull(target.getIdentity().getIdempotenceId());
    }

    @Test
    void created() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getIdentity());
        Duration ageNano = Duration.between(target.getIdentity().getCreated(), Instant.now());
        assertEquals(0, ageNano.getSeconds(), "Event to old " + ageNano);
        assertTrue(ageNano.getNano() >= 0, "Event to young " + ageNano);
    }

    @Test
    void createdLocal() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getIdentity());
        Duration ageNano = Duration.between(target.getIdentity().getCreatedLocal(), LocalDateTime.now());
        assertEquals(0, ageNano.getSeconds(), "Event to old " + ageNano);
        assertTrue(ageNano.getNano() >= 0, "Event to young " + ageNano);
    }

    @Test
    void createdZoned() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getIdentity());
        Duration ageNano = Duration.between(target.getIdentity().getCreatedZoned(), ZonedDateTime.now());
        assertEquals(0, ageNano.getSeconds(), "Event to old " + ageNano);
        assertTrue(ageNano.getNano() >= 0, "Event to young " + ageNano);
    }

    @Test
    void publisher() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getPublisher());
        assertEquals(SimpleBuilder.create().getSystemName(), target.getPublisher().getSystem());
        assertEquals(SimpleBuilder.create().getServiceName(), target.getPublisher().getService());
    }

    @Test
    void type() {
        DomainEvent target = SimpleBuilder.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getType());
        assertEquals("SimpleEvent", target.getType().getName());
        assertEquals("1.0.0", target.getType().getVersion());
    }

    @Test
    void idempotenceId() {
        String idempotenceId = "test-ABC";
        DomainEvent target = SimpleBuilder.create()
                .idempotenceId(idempotenceId)
                .build();
        assertEquals(idempotenceId, target.getIdentity().getIdempotenceId());
    }

    @Test
    void eventTypeWithGeneratedVersion() {
        DomainEvent target = SimpleBuilderWithGeneratedVersion.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getType());
        assertEquals("SimpleEventWithGeneratedVersion", target.getType().getName());
        assertEquals("4.5.6", target.getType().getVersion());
    }

    @Test
    void commandTypeWithGeneratedVersion() {
        AvroMessage target = SimpleCommandBuilderWithGeneratedVersion.create().idempotenceId("idempotenceId").build();
        assertNotNull(target.getType());
        assertEquals("SimpleCommandWithGeneratedVersion", target.getType().getName());
        assertEquals("7.8.9", target.getType().getVersion());
    }

    @Data
    private static class SimpleEvent implements AvroDomainEvent {
        private Schema schema = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SimpleEvent\",\"namespace\":\"ch.admin.bit.jeap.domainevent.avro.event.protocol\",\"fields\":[]}");
        private String domainEventVersion;
        private AvroDomainEventIdentity identity;
        private AvroDomainEventPublisher publisher;
        private AvroDomainEventType type;
        private MessageReferences references;
        private byte[] serializedMessage;
    }

    @Getter
    private static class SimpleBuilder extends AvroDomainEventBuilder<SimpleBuilder, SimpleEvent> {
        private final String systemName = "DomainEventTest";
        private final String serviceName = "IdlTest";
        private final String specifiedMessageTypeVersion = "1.0.0";

        private SimpleBuilder() {
            super(SimpleEvent::new);
        }

        static SimpleBuilder create() {
            return new SimpleBuilder();
        }

        @Override
        protected SimpleBuilder self() {
            return this;
        }

        @Override
        public SimpleEvent build() {
            setReferences(mock(MessageReferences.class));
            return super.build();
        }
    }

    @Data
    private static class SimpleEventWithGeneratedVersion implements AvroDomainEvent {
        public static final String MESSAGE_TYPE_VERSION$ = "4.5.6";

        private Schema schema = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SimpleEventWithGeneratedVersion\",\"namespace\":\"ch.admin.bit.jeap.domainevent.avro.event.protocol\",\"fields\":[]}");
        private String domainEventVersion;
        private AvroDomainEventIdentity identity;
        private AvroDomainEventPublisher publisher;
        private AvroDomainEventType type;
        private MessageReferences references;
        private byte[] serializedMessage;
    }

    @Getter
    private static class SimpleBuilderWithGeneratedVersion extends AvroDomainEventBuilder<SimpleBuilderWithGeneratedVersion, SimpleEventWithGeneratedVersion> {
        private final String systemName = "DomainEventTest";
        private final String serviceName = "IdlTest";

        private SimpleBuilderWithGeneratedVersion() {
            super(SimpleEventWithGeneratedVersion::new);
        }

        static SimpleBuilderWithGeneratedVersion create() {
            return new SimpleBuilderWithGeneratedVersion();
        }

        @Override
        protected SimpleBuilderWithGeneratedVersion self() {
            return this;
        }

        @Override
        public SimpleEventWithGeneratedVersion build() {
            setReferences(mock(MessageReferences.class));
            return super.build();
        }
    }

    @Data
    private static class SimpleCommandWithGeneratedVersion implements AvroCommand {
        public static final String MESSAGE_TYPE_VERSION$ = "7.8.9";

        private Schema schema = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SimpleCommandWithGeneratedVersion\",\"namespace\":\"ch.admin.bit.jeap.domainevent.avro.event.protocol\",\"fields\":[]}");
        private String commandVersion;
        private AvroMessageIdentity identity;
        private AvroMessagePublisher publisher;
        private AvroMessageType type;
        private MessageReferences references;
        private byte[] serializedMessage;
    }

    @Getter
    private static class SimpleCommandBuilderWithGeneratedVersion extends AvroCommandBuilder<SimpleCommandBuilderWithGeneratedVersion, SimpleCommandWithGeneratedVersion> {
        private final String systemName = "DomainEventTest";
        private final String serviceName = "IdlTest";

        private SimpleCommandBuilderWithGeneratedVersion() {
            super(SimpleCommandWithGeneratedVersion::new);
        }

        static SimpleCommandBuilderWithGeneratedVersion create() {
            return new SimpleCommandBuilderWithGeneratedVersion();
        }

        @Override
        protected SimpleCommandBuilderWithGeneratedVersion self() {
            return this;
        }

        @Override
        public SimpleCommandWithGeneratedVersion build() {
            setReferences(mock(MessageReferences.class));
            return super.build();
        }
    }
}
