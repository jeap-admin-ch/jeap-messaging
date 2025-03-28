package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractsByTemplates;
import ch.admin.bit.jeap.messaging.annotations.processor.util.AvroClassFinder;
import ch.admin.bit.jeap.messaging.annotations.processor.util.TemplateMessageCollector;
import ch.admin.bit.jeap.messaging.annotations.processor.util.TemplatePathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JeapMessageTemplateAnnotationProcessorTest {

    private JeapMessageTemplateAnnotationProcessor processor;
    private RoundEnvironment roundEnv;
    private Messager messager;
    private AvroClassFinder avroClassFinder;
    private TemplatePathResolver templatePathResolver;
    private TemplateMessageCollector templateMessageCollector;

    @BeforeEach
    void setUp() throws Exception {
        ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
        roundEnv = mock(RoundEnvironment.class);
        messager = mock(Messager.class);
        avroClassFinder = mock(AvroClassFinder.class);
        templatePathResolver = mock(TemplatePathResolver.class);
        templateMessageCollector = mock(TemplateMessageCollector.class);

        when(processingEnv.getMessager()).thenReturn(messager);

        processor = new JeapMessageTemplateAnnotationProcessor();
        processor.init(processingEnv);

        // Use reflection to set the private fields
        setPrivateField(processor, "avroClassFinder", avroClassFinder);
        setPrivateField(processor, "templatePathResolver", templatePathResolver);
        setPrivateField(processor, "templateMessageCollector", templateMessageCollector);

        // Reset the static variable
        setStaticField();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void setStaticField() throws Exception {
        Field field = JeapMessageTemplateAnnotationProcessor.class.getDeclaredField("alreadyProcessed");
        field.setAccessible(true);
        field.set(null, false);
    }

    @Test
    void testProcessWithValidAnnotations() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);
        JeapMessageConsumerContractsByTemplates annotationInstance = mock(JeapMessageConsumerContractsByTemplates.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(annotatedElement.getAnnotation(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(annotationInstance);
        when(annotationInstance.appName()).thenReturn("TestApp");

        Set<Class<?>> annotatedClasses = new HashSet<>(Collections.singletonList(Object.class));
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(annotatedClasses);
        when(templatePathResolver.getTemplatePath(annotatedElement)).thenReturn("/path/to/templates");

        Map<String, Set<String>> templateMessages = new HashMap<>();
        templateMessages.put("TestMessage", Set.of("topic1", "topic2"));
        when(templateMessageCollector.collectTemplateMessages(anyString(), any())).thenReturn(templateMessages);

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
        verify(messager, never()).printMessage(eq(Diagnostic.Kind.ERROR), anyString());
    }

    @Test
    void testProcessWithoutAnnotations() {
        when(roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(Collections.emptySet());

        boolean result = processor.process(Collections.emptySet(), roundEnv);

        assertFalse(result);
        verify(messager, never()).printMessage(eq(Diagnostic.Kind.ERROR), anyString());
    }

    @Test
    void testProcessWithNoTemplatePath() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(templatePathResolver.getTemplatePath(annotatedElement)).thenReturn(null);

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
    }

    @Test
    void testProcessWithNoAnnotatedClasses() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(templatePathResolver.getTemplatePath(annotatedElement)).thenReturn("/path/to/templates");
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(Collections.emptySet());

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
    }
}
