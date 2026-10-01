package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import org.apache.avro.Schema;
import org.apache.avro.specific.AvroGenerated;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvroSchemaExtractorTest {

    private static final String SCHEMA_JSON = """
            {"type":"record","name":"TestMessage","namespace":"ch.admin.bit.jeap.test","fields":[{"name":"field","type":"string"}]}""";

    private final ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);

    @Test
    void extractSchemaJson_whenClassOnClasspathProvidesGetClassSchema_thenReadsSchemaFromClass() {
        Optional<String> schemaJson = AvroSchemaExtractor.extractSchemaJson(processingEnv, typeElement(MessageTypeWithClassSchema.class));

        assertThat(schemaJson).contains(MessageTypeWithClassSchema.SCHEMA$.toString());
    }

    @Test
    void extractSchemaJson_whenClassOnClasspathOnlyProvidesSchemaField_thenReadsSchemaFromField() {
        Optional<String> schemaJson = AvroSchemaExtractor.extractSchemaJson(processingEnv, typeElement(MessageTypeWithSchemaFieldOnly.class));

        assertThat(schemaJson).contains(MessageTypeWithSchemaFieldOnly.SCHEMA$.toString());
    }

    @Test
    void extractSchemaJson_whenClassIsNotAvailable_thenReturnsEmptyAndReportsTheReason() {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn("ch.admin.bit.jeap.test.NotOnTheClasspathEvent");
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        when(typeElement.getEnclosedElements()).thenReturn(List.of());
        Messager messager = mock(Messager.class);
        when(processingEnv.getMessager()).thenReturn(messager);

        Optional<String> schemaJson = AvroSchemaExtractor.extractSchemaJson(processingEnv, typeElement);

        assertThat(schemaJson).isEmpty();
        ArgumentCaptor<CharSequence> message = ArgumentCaptor.forClass(CharSequence.class);
        verify(messager, atLeastOnce()).printMessage(eq(Diagnostic.Kind.NOTE), message.capture());
        assertThat(message.getAllValues())
                .anySatisfy(reportedFailure -> assertThat(reportedFailure.toString())
                        .contains("NotOnTheClasspathEvent.SCHEMA$ failed")
                        .contains("ClassNotFoundException"));
    }

    @Test
    void isAvroGeneratedType_whenTypeIsAnnotatedWithAvroGenerated_thenTrue() {
        TypeElement typeElement = mock(TypeElement.class);
        doReturn(List.of(annotationMirror(AvroGenerated.class))).when(typeElement).getAnnotationMirrors();

        assertThat(AvroSchemaExtractor.isAvroGeneratedType(typeElement)).isTrue();
    }

    @Test
    void isAvroGeneratedType_whenTypeDeclaresSchemaField_thenTrue() {
        TypeElement typeElement = mock(TypeElement.class);
        doReturn(List.of()).when(typeElement).getAnnotationMirrors();
        doReturn(List.of(field("SCHEMA$"))).when(typeElement).getEnclosedElements();

        assertThat(AvroSchemaExtractor.isAvroGeneratedType(typeElement)).isTrue();
    }

    @Test
    void isAvroGeneratedType_whenTypeIsNeitherAnnotatedNorDeclaresSchemaField_thenFalse() {
        TypeElement typeElement = mock(TypeElement.class);
        doReturn(List.of(annotationMirror(SuppressWarnings.class))).when(typeElement).getAnnotationMirrors();
        doReturn(List.of(field("MESSAGE_TYPE_NAME"))).when(typeElement).getEnclosedElements();

        assertThat(AvroSchemaExtractor.isAvroGeneratedType(typeElement)).isFalse();
    }

    @Test
    void parsedStringLiterals_whenSchemaIsSplitIntoMultipleLiterals_thenConcatenatesAllLiterals() {
        // avro splits long schema json into multiple string literals: new Schema.Parser().parse("...", "...")
        String firstHalf = SCHEMA_JSON.substring(0, 20);
        String secondHalf = SCHEMA_JSON.substring(20);

        String schemaJson = AvroSchemaExtractor.parsedStringLiterals(parseInvocation(literal(firstHalf), literal(secondHalf)));

        assertThat(schemaJson).isEqualTo(SCHEMA_JSON);
    }

    @Test
    void parsedStringLiterals_whenArgumentIsNotAStringLiteral_thenReturnsNull() {
        String schemaJson = AvroSchemaExtractor.parsedStringLiterals(parseInvocation(mock(ExpressionTree.class)));

        assertThat(schemaJson).isNull();
    }

    @Test
    void parsedStringLiterals_whenInitializerIsNotAMethodInvocation_thenReturnsNull() {
        String schemaJson = AvroSchemaExtractor.parsedStringLiterals(literal(SCHEMA_JSON));

        assertThat(schemaJson).isNull();
    }

    private TypeElement typeElement(Class<?> messageTypeClass) {
        TypeElement typeElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn(messageTypeClass.getName());
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        return typeElement;
    }

    private static MethodInvocationTree parseInvocation(ExpressionTree... arguments) {
        MethodInvocationTree methodInvocation = mock(MethodInvocationTree.class);
        doReturn(List.of(arguments)).when(methodInvocation).getArguments();
        return methodInvocation;
    }

    private static LiteralTree literal(String value) {
        LiteralTree literal = mock(LiteralTree.class);
        when(literal.getValue()).thenReturn(value);
        return literal;
    }

    private static AnnotationMirror annotationMirror(Class<?> annotationClass) {
        TypeElement annotationElement = mock(TypeElement.class);
        Name qualifiedName = mock(Name.class);
        when(qualifiedName.toString()).thenReturn(annotationClass.getName());
        when(annotationElement.getQualifiedName()).thenReturn(qualifiedName);
        DeclaredType annotationType = mock(DeclaredType.class);
        when(annotationType.asElement()).thenReturn(annotationElement);
        AnnotationMirror annotationMirror = mock(AnnotationMirror.class);
        when(annotationMirror.getAnnotationType()).thenReturn(annotationType);
        return annotationMirror;
    }

    private static Element field(String simpleName) {
        Element element = mock(Element.class);
        Name name = mock(Name.class);
        when(name.toString()).thenReturn(simpleName);
        when(element.getSimpleName()).thenReturn(name);
        when(element.getKind()).thenReturn(ElementKind.FIELD);
        return element;
    }

    @SuppressWarnings("unused")
    static class MessageTypeWithClassSchema {
        public static final Schema SCHEMA$ = new Schema.Parser().parse(SCHEMA_JSON);

        public static Schema getClassSchema() {
            return SCHEMA$;
        }
    }

    static class MessageTypeWithSchemaFieldOnly {
        public static final Schema SCHEMA$ = new Schema.Parser().parse(SCHEMA_JSON);
    }
}
