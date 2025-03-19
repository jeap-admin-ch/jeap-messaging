package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationValidatorTest {

    enum SubtypeEnum {TYPE1, TYPE2}

    static class TestSubtypeResolver implements SubTypeResolver<AvroMessage, SubtypeEnum> {

        @Override
        public SubtypeEnum resolveSubType(AvroMessage avroMessage) {
            return SubtypeEnum.TYPE1;
        }
    }

    @Test
    void getSubTypeEnumReturnedBySubtypeResolver_actualTypeReturned() {
        Class<Enum<?>> enumType = ConfigurationValidator.getSubTypeEnumReturnedBySubtypeResolver(new TestSubtypeResolver());
        assertThat(enumType)
                .isSameAs(SubtypeEnum.class);
    }
}
