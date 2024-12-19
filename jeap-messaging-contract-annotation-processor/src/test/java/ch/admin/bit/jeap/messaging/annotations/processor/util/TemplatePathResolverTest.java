package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplatePathResolverTest {

    private Filer filer;
    private Messager messager;
    private TemplatePathResolver resolver;

    @BeforeEach
    void setUp() {
        ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
        filer = mock(Filer.class);
        messager = mock(Messager.class);
        when(processingEnv.getFiler()).thenReturn(filer);
        when(processingEnv.getMessager()).thenReturn(messager);
        resolver = new TemplatePathResolver(processingEnv);
    }

    @Test
    void testGetTemplatePathForSomeJeapClass() throws IOException {
        TypeElement annotatedElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn("ch.admin.bit.jeap.SomeClass");
        when(annotatedElement.getQualifiedName()).thenReturn(qualifiedName);

        FileObject fileObject = mock(FileObject.class);
        when(fileObject.toUri()).thenReturn(URI.create("file:///path/to/target/classes/ch/admin/bit/jeap/SomeClass.class"));
        when(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "ch/admin/bit/jeap/SomeClass.class")).thenReturn(fileObject);

        String templatePath = resolver.getTemplatePath(annotatedElement);

        assertNotNull(templatePath);
        assertTrue(templatePath.endsWith("/path/to/target/classes/process/templates"));
    }


    @Test
    void testGetTemplatePathToUnknownUri() throws IOException {
        TypeElement annotatedElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn("ch.admin.bit.jeap.SomeClass");
        when(annotatedElement.getQualifiedName()).thenReturn(qualifiedName);

        FileObject fileObject = mock(FileObject.class);
        when(fileObject.toUri()).thenReturn(URI.create("this-is-not-a-valid-uri"));
        when(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "ch/admin/bit/jeap/SomeClass.class")).thenThrow(new IOException("Test Exception"));

        String templatePath = resolver.getTemplatePath(annotatedElement);

        assertNull(templatePath);
        verify(messager).printMessage(eq(Diagnostic.Kind.ERROR), contains("Error finding resource: Test Exception"));
    }
}
