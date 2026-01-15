package ch.admin.bit.jeap.messaging.avro;

import ch.admin.bit.jeap.messaging.model.MessagePayload;
import ch.admin.bit.jeap.messaging.model.MessageReferences;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings({"SameReturnValue", "java:S1133", "java:S5738"})
public abstract class AvroMessageBuilder<BuilderType extends AvroMessageBuilder, MessageType extends AvroMessage> {

    protected final Supplier<MessageType> constructor;
    protected String idempotenceId;
    private MessagePayload payload;
    private MessageReferences references;
    private String processId;
    protected String variant;

    protected abstract String getServiceName();

    protected abstract String getSystemName();

    protected abstract BuilderType self();

    protected void setPayload(MessagePayload payload) {
        this.payload = payload;
    }

    protected void setReferences(MessageReferences references) {
        this.references = references;
    }

    protected void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * Allows for explicitly specifying a message type version for use cases where the message type
     * being built is not generated from the message type registry, and its version is thus unknown.
     * See {{@link AvroMessageBuilder#getGeneratedOrSpecifiedVersion(AvroMessage)} for details.
     *
     * @return Message type version to use in the messageType part of the message, i.e. "1.2.3"
     */
    protected String getSpecifiedMessageTypeVersion() {
        return null;
    }

    public BuilderType idempotenceId(String idempotenceId) {
        if (isBlank(idempotenceId)) {
            throw AvroMessageBuilderException.propertyValue("identity.idempotenceId", idempotenceId);
        }
        this.idempotenceId = idempotenceId;
        return self();
    }

    public BuilderType variant(String variant) {
        if (isBlank(variant)) {
            throw AvroMessageBuilderException.propertyValue("type.variant", variant);
        }

        org.apache.avro.Schema schema = retrieveSchema();
        if (schema.getField("type").schema().getField("variant") == null) {
            throw AvroMessageBuilderException.variantNotAvailableInSchema(constructor.get().getClass());
        }

        this.variant = variant;
        return self();
    }

    private org.apache.avro.Schema retrieveSchema() {
        try {
            Field schemaField = constructor.get().getClass().getDeclaredField("SCHEMA$");
            return (org.apache.avro.Schema) schemaField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw AvroMessageBuilderException.schemaNotAvailable(constructor.get().getClass());
        }
    }

    protected void checkMandatoryFields() {
        if (isBlank(idempotenceId)) {
            throw AvroMessageBuilderException.propertyValue("identity.idempotenceId", idempotenceId);
        }
        String serviceName = getServiceName();
        if (isBlank(serviceName)) {
            throw AvroMessageBuilderException.propertyValue("publisher.serviceName", serviceName);
        }
        String systemName = getSystemName();
        if (isBlank(systemName)) {
            throw AvroMessageBuilderException.propertyValue("publisher.systemName", systemName);
        }
    }

    /**
     * Returns in order of precedence:
     * <ol>
     *     <li>The message type version as found in the generated avro java binding</li>
     *     <li>The message type version returned by getSpecifiedMessageTypeVersion()</li>
     *     <li>null if both contain no value</li>
     * </ol>
     */
    protected String getGeneratedOrSpecifiedVersion(AvroMessage message) {
        String version = MessageVersionAccessor.getGeneratedVersion(message.getClass());
        if (version == null) {
            version = getSpecifiedMessageTypeVersion();
        }
        return version;
    }

    protected void addCommon(MessageType message) {
        if (references != null) {
            message.setReferences(references);
        }

        if (payload != null) {
            message.setPayload(payload);
        }

        if (processId != null) {
            message.setProcessId(processId);
        }
    }

    protected static boolean isBlank(final CharSequence cs) {
        final int strLen = cs == null ? 0 : cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
