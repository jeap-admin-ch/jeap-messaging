package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TypeRefFinderTest {

    private ProcessingEnvironment processingEnv;
    private TypeMirror typeMirror;

    @BeforeEach
    public void setUp() {
        processingEnv = mock(ProcessingEnvironment.class);
        Elements elementUtils = mock(Elements.class);
        TypeElement typeElement = mock(TypeElement.class);
        typeMirror = mock(TypeMirror.class);

        when(processingEnv.getElementUtils()).thenReturn(elementUtils);
        when(elementUtils.getTypeElement(anyString())).thenReturn(typeElement);
        when(typeElement.asType()).thenReturn(typeMirror);
    }

    @Test
    public void testFindTypeRefOfClassByShortName_Found() {
        Set<Class<?>> classes = Collections.singleton(TestClass.class);
        TypeMirror result = TypeRefFinder.findTypeRefOfClassByShortName(processingEnv, classes, "TestClass");

        assertNotNull(result);
        assertEquals(typeMirror, result);
    }

    @Test
    public void testFindTypeRefOfClassByShortName_NotFound() {
        Set<Class<?>> classes = Collections.singleton(OtherClass.class);
        TypeMirror result = TypeRefFinder.findTypeRefOfClassByShortName(processingEnv, classes, "TestClass");

        assertNull(result);
    }

    @Test
    public void testFindClassByShortName_Found() {
        Set<Class<?>> classes = Collections.singleton(TestClass.class);
        Class<?> result = TypeRefFinder.findClassByShortName(classes, "TestClass");

        assertNotNull(result);
        assertEquals(TestClass.class, result);
    }

    @Test
    public void testFindClassByShortName_NotFound() {
        Set<Class<?>> classes = Collections.singleton(OtherClass.class);
        Class<?> result = TypeRefFinder.findClassByShortName(classes, "TestClass");

        assertNull(result);
    }

    public static class TestClass {
        public static class TypeRef {}
    }

    public static class OtherClass {}
}
