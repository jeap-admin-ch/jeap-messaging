package ch.admin.bit.jeap.domainevent.avro;

import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilder;
import ch.admin.bit.jeap.messaging.avro.AvroMessageBuilderException;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"java:S1133", "java:S5738"}) // Deprecation
public abstract class AvroDomainEventBuilder<BuilderType extends AvroDomainEventBuilder, EventType extends AvroDomainEvent> extends AvroMessageBuilder<BuilderType, EventType> {

    private final static String DOMAIN_EVENT_VERSION = "1.3.0";
    private AvroDomainEventUser user;

    protected AvroDomainEventBuilder(Supplier<EventType> constructor) {
        super(constructor);
    }

    /**
     * Version of the event type to create. Not to be confused with {@link #DOMAIN_EVENT_VERSION} which is the
     * Version of the DomainEvent-Structure.
     * The builder uses the event version from the generated event avro binding, no need to override. Will be removed
     * in a future release.
     *
     * @deprecated When generating jEAP messages from the message type registry, the version is automatically set
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(forRemoval = true, since = "3.7.0")
    protected String getEventVersion() {
        return null; // default to version from message type registry in generated avro event java binding
    }

    public BuilderType user(AvroDomainEventUser user) {
        this.user = user;
        return self();
    }

    public EventType build() {
        checkMandatoryFields();

        EventType event = constructor.get();
        event.setIdentity(buildIdentity());
        event.setPublisher(buildPublisher());
        event.setType(buildType(event));
        event.setDomainEventVersion(DOMAIN_EVENT_VERSION);
        if (user != null) {
            event.setUser(user);
        }
        addCommon(event);
        return event;
    }

    private AvroDomainEventIdentity buildIdentity() {
        AvroDomainEventIdentity identity = new AvroDomainEventIdentity();
        identity.setCreated(Instant.now());
        identity.setEventId(UUID.randomUUID().toString());
        identity.setIdempotenceId(this.idempotenceId);
        return identity;
    }

    private AvroDomainEventPublisher buildPublisher() {
        String serviceName = getServiceName();
        String systemName = getSystemName();
        AvroDomainEventPublisher publisher = new AvroDomainEventPublisher();
        publisher.setService(serviceName);
        publisher.setSystem(systemName);
        return publisher;
    }

    private AvroDomainEventType buildType(EventType event) {
        String name = event.getSchema().getName();
        String version = getGeneratedOrSpecifiedVersion(event);
        if (version == null) {
            version = getEventVersion();
        }
        if (isBlank(version)) {
            throw AvroMessageBuilderException.propertyValue("type.version", version);
        }
        AvroDomainEventType type = new AvroDomainEventType();
        type.setName(name);
        type.setVersion(version);
        type.setVariant(variant);
        return type;
    }
}
