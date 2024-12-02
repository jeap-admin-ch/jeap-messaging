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
}
