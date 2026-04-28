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
import static org.mockito.ArgumentMatchers.*;
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

        setPrivateField(processor, "avroClassFinder", avroClassFinder);
        setPrivateField(processor, "templatePathResolver", templatePathResolver);
        setPrivateField(processor, "templateMessageCollector", templateMessageCollector);

        resetAlreadyProcessed();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void resetAlreadyProcessed() throws Exception {
        Field field = JeapMessageTemplateAnnotationProcessor.class.getDeclaredField("alreadyProcessed");
        field.setAccessible(true);
        field.set(null, false);
    }

    @Test
    void processWithValidAnnotations_collectsMessagesAndGeneratesContracts() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);
        JeapMessageConsumerContractsByTemplates annotationInstance = mock(JeapMessageConsumerContractsByTemplates.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(annotatedElement.getAnnotation(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(annotationInstance);
        when(annotationInstance.appName()).thenReturn("TestApp");
        when(annotationInstance.templatesPath()).thenReturn("opensearch");

        Set<Class<?>> annotatedClasses = new HashSet<>(Collections.singletonList(Object.class));
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(annotatedClasses);
        when(templatePathResolver.getTemplatePath(annotatedElement, "opensearch"))
                .thenReturn("/path/to/templates");

        Map<String, Set<String>> templateMessages = new HashMap<>();
        templateMessages.put("TestMessage", Set.of("topic1", "topic2"));
        when(templateMessageCollector.collectTemplateMessages(eq("/path/to/templates"), any()))
                .thenReturn(templateMessages);

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
        verify(templateMessageCollector).collectTemplateMessages(eq("/path/to/templates"), any());
        verify(messager, never()).printMessage(eq(Diagnostic.Kind.ERROR), anyString());
    }

    @Test
    void processWithoutAnnotatedElements_returnsFalse() {
        when(roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(Collections.emptySet());

        boolean result = processor.process(Collections.emptySet(), roundEnv);

        assertFalse(result);
        verify(messager, never()).printMessage(eq(Diagnostic.Kind.ERROR), anyString());
    }

    @Test
    void processWithNullTemplatePath_skipsMessageCollection() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);
        JeapMessageConsumerContractsByTemplates annotationInstance = mock(JeapMessageConsumerContractsByTemplates.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(annotatedElement.getAnnotation(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(annotationInstance);
        when(annotationInstance.templatesPath()).thenReturn("custom/path");

        Set<Class<?>> annotatedClasses = new HashSet<>(Collections.singletonList(Object.class));
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(annotatedClasses);
        when(templatePathResolver.getTemplatePath(annotatedElement, "custom/path")).thenReturn(null);

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
        verify(templateMessageCollector, never()).collectTemplateMessages(anyString(), any());
    }

    @Test
    void processWithNoAnnotatedClasses_skipsContractGeneration() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(Collections.emptySet());

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
        verify(templatePathResolver, never()).getTemplatePath(any(), any());
        verify(templateMessageCollector, never()).collectTemplateMessages(anyString(), any());
    }

    @Test
    void processWithEmptyMessages_skipsContractGeneration() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);
        JeapMessageConsumerContractsByTemplates annotationInstance = mock(JeapMessageConsumerContractsByTemplates.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(annotatedElement.getAnnotation(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(annotationInstance);
        when(annotationInstance.appName()).thenReturn("TestApp");
        when(annotationInstance.templatesPath()).thenReturn("/some/path");

        Set<Class<?>> annotatedClasses = new HashSet<>(Collections.singletonList(Object.class));
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(annotatedClasses);
        when(templatePathResolver.getTemplatePath(annotatedElement, "/some/path")).thenReturn("/resolved/path");
        when(templateMessageCollector.collectTemplateMessages(eq("/resolved/path"), any()))
                .thenReturn(Collections.emptyMap());

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
        verify(messager, never()).printMessage(any(), anyString());
    }

    @Test
    void processWhenNoTypeRefFoundForMessage_printsWarning() {
        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);
        JeapMessageConsumerContractsByTemplates annotationInstance = mock(JeapMessageConsumerContractsByTemplates.class);

        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);
        when(annotatedElement.getAnnotation(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(annotationInstance);
        when(annotationInstance.appName()).thenReturn("TestApp");
        when(annotationInstance.templatesPath()).thenReturn("/some/path");

        // Object.class has no TypeRef inner class → findTypeRefOfClassByShortName returns null
        Set<Class<?>> annotatedClasses = new HashSet<>(Collections.singletonList(Object.class));
        when(avroClassFinder.getAvroGeneratedClasses()).thenReturn(annotatedClasses);
        when(templatePathResolver.getTemplatePath(annotatedElement, "/some/path")).thenReturn("/resolved/path");

        Map<String, Set<String>> messages = new HashMap<>();
        messages.put("Object", Set.of("topic1")); // matches Object.class simple name but has no TypeRef
        when(templateMessageCollector.collectTemplateMessages(eq("/resolved/path"), any()))
                .thenReturn(messages);

        boolean result = processor.process(Collections.singleton(annotation), roundEnv);

        assertTrue(result);
        verify(messager).printMessage(eq(Diagnostic.Kind.WARNING), contains("Object"));
    }

    @Test
    void processOnSecondCall_returnsImmediatelyWithoutProcessing() {
        when(roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class))
                .thenReturn(Collections.emptySet());
        processor.process(Collections.emptySet(), roundEnv); // first call

        TypeElement annotation = mock(TypeElement.class);
        Element annotatedElement = mock(Element.class);
        doReturn(Collections.singleton(annotatedElement))
                .when(roundEnv).getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class);

        boolean result = processor.process(Collections.singleton(annotation), roundEnv); // second call

        assertFalse(result);
        verify(avroClassFinder, never()).getAvroGeneratedClasses();
    }
}

