package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.apache.avro.specific.AvroGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import javax.annotation.processing.Messager;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvroClassFinderTest {

    private AvroClassFinder avroClassFinder;

    @BeforeEach
    void setUp() {
        Messager messager = mock(Messager.class);
        avroClassFinder = new AvroClassFinder(messager);
    }

    @Test
    void testGetAvroGeneratedClasses() {
        Reflections reflections = mock(Reflections.class);
        Set<Class<?>> mockClasses = Set.of(AvroGenerated.class);

        when(reflections.getTypesAnnotatedWith(AvroGenerated.class)).thenReturn(mockClasses);

        Set<Class<?>> result = avroClassFinder.getAvroGeneratedClasses();
        //There are more than just the mock classes, which are annotated with @AvroGenerated
        assertFalse(result.isEmpty());
    }

}
