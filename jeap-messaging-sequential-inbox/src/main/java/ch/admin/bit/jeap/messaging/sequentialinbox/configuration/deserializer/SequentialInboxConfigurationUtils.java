package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import lombok.experimental.UtilityClass;

@UtilityClass
class SequentialInboxConfigurationUtils {

    <T> T newInstance(String className, Class<?> expectedSupertype) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) Class.forName(className);
            if (!expectedSupertype.isAssignableFrom(clazz)) {
                throw SequentialInboxConfigurationException.badInstanceType(clazz, expectedSupertype);
            }

            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw SequentialInboxConfigurationException.errorWhileCreatingInstance(className, e);
        }
    }
}
