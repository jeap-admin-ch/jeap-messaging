package ch.admin.bit.jeap.messaging.kafka.properties;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PropertyRequirements {

    /**
     * While it generally does not make sense to set kafka config prop values to null,
     * {@link org.springframework.kafka.core.DefaultKafkaConsumerFactory} uses a concurrent hashmap internally which
     * does not allow null values. Avoid the NPE in this case and provide a helpful error message instead:
     */
    public static <T> T requireNonNullValue(String key, T value) {
        if (value == null) {
            throw new IllegalArgumentException("Required kafka config property " + key + " is set to null. Please provide a valid configuration property value");
        }
        return value;
    }
}
