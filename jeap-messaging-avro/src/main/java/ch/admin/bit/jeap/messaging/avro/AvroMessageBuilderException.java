package ch.admin.bit.jeap.messaging.avro;

@SuppressWarnings("WeakerAccess")
public class AvroMessageBuilderException extends RuntimeException {

    protected AvroMessageBuilderException(String message) {
        super(message);
    }

    public static AvroMessageBuilderException eventHasNotPayload(Class<?> klass) {
        String message = String.format("The event '%s' has no payload", klass.getCanonicalName());
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException eventHasNotReferences(Class<?> klass) {
        String message = String.format("The event '%s' has no references", klass.getCanonicalName());
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException propertyNull(String propertyName) {
        String message = String.format("The field '%s' is not allowed to be null", propertyName);
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException propertyValue(String propertyName, Object value) {
        if (value == null) {
            return propertyNull(propertyName);
        }
        String message = String.format("The field '%s' is not allowed to be '%s'", propertyName, value);
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException illegalArgument(String propertyName, String condition) {
        String message = String.format("The field '%s' is not set properly: %s", propertyName, condition);
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException onlyImplementedAfter(String field, String version) {
        String message = String.format("The field '%s' is only implemented in DomainEvent version %s or after", field, version);
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException userFieldNotDefined(Class<?> klass) {
        String message = String.format("The message class '%s' does not have a user field.", klass.getCanonicalName());
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException variantNotAvailableInSchema(Class<?> klass) {
        String message = String.format("The schema of the message type '%s' does not have a 'variant' field. In order to use the 'variant' field in this message type, a new version of the message must be defined in order to generate the schema again.", klass.getCanonicalName());
        return new AvroMessageBuilderException(message);
    }

    public static AvroMessageBuilderException schemaNotAvailable(Class<?> klass) {
        String message = String.format("The generated class for '%s' does not have a 'SCHEMA$' field.", klass.getCanonicalName());
        return new AvroMessageBuilderException(message);
    }
}
