package ch.admin.bit.jeap.command.avro;

import ch.admin.bit.jeap.messaging.avro.*;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"java:S1133", "java:S5738"}) // Deprecation
public abstract class AvroCommandBuilder<BuilderType extends AvroCommandBuilder, CommandType extends AvroCommand> extends AvroMessageBuilder<BuilderType, CommandType> {

    private final static String COMMAND_VERSION = "1.2.0";
    private AvroMessageUser user;

    protected AvroCommandBuilder(Supplier<CommandType> constructor) {
        super(constructor);
    }

    /**
     * Version of the command type to create. Not to be confused with {@link #COMMAND_VERSION} which is the
     * Version of the Command-Structure (like DomainEventVersion)
     * The builder uses the event version from the generated event avro binding, no need to override. Will be removed
     * in a future release.
     *
     * @deprecated When generating jEAP messages from the message type registry, the version is automatically set
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated(forRemoval = true, since = "3.7.0")
    protected String getCommandTypeVersion() {
        return null; // default to version from message type registry in generated avro command java binding
    }

    public BuilderType user(AvroMessageUser user) {
        this.user = user;
        return self();
    }

    public CommandType build() {
        CommandType command = constructor.get();
        command.setIdentity(buildIdentity());
        command.setPublisher(buildPublisher());
        command.setType(buildType(command));
        command.setCommandVersion(COMMAND_VERSION);
        if (user != null) {
            command.setUser(user);
        }
        addCommon(command);
        return command;
    }

    private AvroMessageIdentity buildIdentity() {
        AvroMessageIdentity identity = new AvroMessageIdentity();
        identity.setCreated(Instant.now());
        identity.setId(UUID.randomUUID().toString());
        identity.setIdempotenceId(this.idempotenceId);
        return identity;
    }

    private AvroMessagePublisher buildPublisher() {
        String serviceName = getServiceName();
        String systemName = getSystemName();
        AvroMessagePublisher publisher = new AvroMessagePublisher();
        publisher.setService(serviceName);
        publisher.setSystem(systemName);
        return publisher;
    }

    private AvroMessageType buildType(CommandType command) {
        String name = command.getSchema().getName();
        String version = getGeneratedOrSpecifiedVersion(command);
        if (version == null) {
            version = getCommandTypeVersion();
        }
        if (isBlank(version)) {
            throw AvroMessageBuilderException.propertyValue("type.version", version);
        }
        AvroMessageType type = new AvroMessageType();
        type.setName(name);
        type.setVersion(version);
        type.setVariant(variant);
        return type;
    }

}
