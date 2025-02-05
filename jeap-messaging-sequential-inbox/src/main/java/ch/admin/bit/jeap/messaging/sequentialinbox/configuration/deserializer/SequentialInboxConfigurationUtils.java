package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import lombok.experimental.UtilityClass;

@UtilityClass
class SequentialInboxConfigurationUtils {

    <T> T newInstance(String className) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> conditionClass = (Class<T>) Class.forName(className);
            return conditionClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw SequentialInboxConfigurationException.errorWhileCreatingInstance(className, e);
        }
    }
}