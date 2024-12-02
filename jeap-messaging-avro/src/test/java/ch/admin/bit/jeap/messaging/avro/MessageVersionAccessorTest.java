package ch.admin.bit.jeap.messaging.avro;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageVersionAccessorTest {

    @Test
    void getGeneratedVersion() {
        // not cached
        assertNull(MessageVersionAccessor.getGeneratedVersion(Object.class));
        // cached
        assertNull(MessageVersionAccessor.getGeneratedVersion(Object.class));

        // not cached
        assertEquals(ClassWithVersionField.MESSAGE_TYPE_VERSION$,
                MessageVersionAccessor.getGeneratedVersion(ClassWithVersionField.class));
        // cached
        assertEquals(ClassWithVersionField.MESSAGE_TYPE_VERSION$,
                MessageVersionAccessor.getGeneratedVersion(ClassWithVersionField.class));

        assertTrue(MessageVersionAccessor.MESSAGE_TYPE_VERSION_CACHE.containsKey(Object.class));
        assertTrue(MessageVersionAccessor.MESSAGE_TYPE_VERSION_CACHE.containsKey(ClassWithVersionField.class));
    }

    private static class ClassWithVersionField {
        public static final String MESSAGE_TYPE_VERSION$ = "1.2.3";
    }
}