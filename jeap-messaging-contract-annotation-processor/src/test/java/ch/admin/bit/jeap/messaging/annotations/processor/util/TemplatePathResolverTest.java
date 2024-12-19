package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplatePathResolverTest {

    private Filer filer;
    private TemplatePathResolver resolver;

    @BeforeEach
    void setUp() {
        ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
        filer = mock(Filer.class);
        Messager messager = mock(Messager.class);
        when(processingEnv.getFiler()).thenReturn(filer);
        when(processingEnv.getMessager()).thenReturn(messager);
        resolver = new TemplatePathResolver(processingEnv);
    }

    @Test
    void testGetTemplatePathFromMainJavaInCompileContext() throws IOException {
        TypeElement annotatedElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn("ch.admin.bit.jeap.SomeClass");
        when(annotatedElement.getQualifiedName()).thenReturn(qualifiedName);

        FileObject fileObject = mock(FileObject.class);
        when(fileObject.toUri()).thenReturn(URI.create("file:///path/to/src/main/java/ch/admin/bit/jeap/SomeClass.java"));
        when(filer.getResource(StandardLocation.SOURCE_PATH, "", "ch/admin/bit/jeap/SomeClass.java")).thenReturn(fileObject);

        String templatePath = resolver.getTemplatePath(annotatedElement);

        assertNotNull(templatePath);
        assertEquals("/path/to/src/main/resources/process/templates", templatePath);
    }

    @Test
    void testGetTemplatePathFromTestJavaInCompileContext() throws IOException {
        TypeElement annotatedElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn("ch.admin.bit.jeap.SomeClass");
        when(annotatedElement.getQualifiedName()).thenReturn(qualifiedName);

        FileObject fileObject = mock(FileObject.class);
        when(fileObject.toUri()).thenReturn(URI.create("file:///path/to/src/test/java/ch/admin/bit/jeap/SomeClass.java"));
        when(filer.getResource(StandardLocation.SOURCE_PATH, "", "ch/admin/bit/jeap/SomeClass.java")).thenReturn(fileObject);

        String templatePath = resolver.getTemplatePath(annotatedElement);

        assertNotNull(templatePath);
        assertEquals("/path/to/src/test/resources/process/templates", templatePath);
    }


    @Test
    void testGetTemplatePathFromRuntimeEnvironment() throws IOException {
        TypeElement annotatedElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn("ch.admin.bit.jeap.SomeClass");
        when(annotatedElement.getQualifiedName()).thenReturn(qualifiedName);

        when(filer.getResource(StandardLocation.SOURCE_PATH, "", "ch/admin/bit/jeap/SomeClass.java")).thenThrow(new IOException("Test Exception"));

        String templatePath = resolver.getTemplatePath(annotatedElement);

        assertNotNull(templatePath);
        assertEquals(Paths.get(".").toAbsolutePath().normalize().toString(), templatePath);
    }
}
