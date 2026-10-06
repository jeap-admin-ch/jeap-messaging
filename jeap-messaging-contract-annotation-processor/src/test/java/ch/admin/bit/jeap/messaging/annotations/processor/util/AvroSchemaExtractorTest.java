package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.Trees;
import org.apache.avro.Schema;
import org.apache.avro.specific.AvroGenerated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

import javax.annotation.processing.Messager;
import javax.annotation.processing.Filer;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AvroSchemaExtractorTest {

    private static final String SCHEMA_JSON = """
            {"type":"record","name":"TestMessage","namespace":"ch.admin.bit.jeap.test","fields":[{"name":"field","type":"string"}]}""";

    private final ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
    private MockedStatic<Trees> trees;

    @BeforeEach
    void compilerClasspath() throws Exception {
        trees = mockStatic(Trees.class);
        trees.when(() -> Trees.instance(processingEnv)).thenReturn(mock(Trees.class));
        Thread compilerThread = Thread.currentThread();
        Elements elements = mock(Elements.class);
        when(processingEnv.getElementUtils()).thenReturn(elements);
        when(elements.getBinaryName(any())).thenAnswer(call -> ((TypeElement) call.getArgument(0)).getQualifiedName());
        Filer filer = mock(Filer.class);
        when(processingEnv.getFiler()).thenReturn(filer);
        when(filer.getResource(eq(StandardLocation.CLASS_PATH), anyString(), anyString())).thenAnswer(call -> {
            assertThat(Thread.currentThread()).isSameAs(compilerThread);
            String resource = call.<String>getArgument(1).replace('.', '/') + "/" + call.<String>getArgument(2);
            FileObject file = mock(FileObject.class);
            when(file.openInputStream()).thenAnswer(ignored -> {
                var stream = getClass().getClassLoader().getResourceAsStream(resource);
                if (stream == null) {
                    throw new FileNotFoundException(resource);
                }
                return stream;
            });
            return file;
        });
        when(processingEnv.getMessager()).thenReturn(mock(Messager.class));
    }

    @AfterEach
    void closeCompilation() {
        AvroSchemaExtractor.processingOver(processingEnv);
        trees.close();
    }

    @Test
    void virtualMachineErrorPropagatesAndStopsWorker() {
        assertThatThrownBy(() -> AvroSchemaExtractor.extractSchemaJson(processingEnv, typeElement(FatalMessage.class)))
                .isInstanceOf(OutOfMemoryError.class).hasMessage("fatal fixture");
        assertNoWorker();
    }

    @Test
    void threadDeathPropagatesAndStopsWorker() {
        assertThatThrownBy(() -> AvroSchemaExtractor.extractSchemaJson(processingEnv, typeElement(ThreadDeathMessage.class)))
                .isInstanceOf(ThreadDeath.class);
        assertNoWorker();
    }

    @Test
    void sourceRootWithoutTreeNeverLoadsStaleClass() {
        TypeElement type = typeElement(MessageTypeWithClassSchema.class);
        AvroSchemaExtractor.round(processingEnv, java.util.Set.of(type));
        assertThat(AvroSchemaExtractor.extractSchemaJson(processingEnv, type)).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(processingEnv.getFiler());
    }

    private static void assertNoWorker() {
        assertThat(Thread.getAllStackTraces().keySet()).noneMatch(thread -> thread.getName().equals("jeap-schema-extraction"));
    }

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
        when(processingEnv.getElementUtils().getTypeElement(qualifiedName)).thenReturn(typeElement);
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
    void inconsistentCompiledMetadataPreventsUploadingWrongSchema() {
        TypeElement metadata = typeElement(MessageTypeWithClassSchema.TypeRef.class);
        VariableElement constant = mock(VariableElement.class);
        Name name = mock(Name.class);
        when(name.toString()).thenReturn("MESSAGE_TYPE_VERSION");
        when(constant.getSimpleName()).thenReturn(name);
        when(constant.getConstantValue()).thenReturn("different compiler metadata");
        doReturn(List.of(constant)).when(metadata).getEnclosedElements();

        assertThat(AvroSchemaExtractor.extractSchemaJson(processingEnv, typeElement(MessageTypeWithClassSchema.class), metadata)).isEmpty();
        ArgumentCaptor<CharSequence> message = ArgumentCaptor.forClass(CharSequence.class);
        verify(processingEnv.getMessager(), atLeastOnce()).printMessage(eq(Diagnostic.Kind.NOTE), message.capture());
        assertThat(message.getAllValues()).anySatisfy(diagnostic -> assertThat(diagnostic.toString()).contains("metadata differs"));
    }

    @Test
    void isAvroGeneratedType_whenTypeIsAnnotatedWithAvroGenerated_thenTrue() {
        TypeElement typeElement = mock(TypeElement.class);
        when(typeElement.getAnnotation(AvroGenerated.class)).thenReturn(mock(AvroGenerated.class));

        assertThat(AvroSchemaExtractor.isAvroGeneratedType(typeElement)).isTrue();
    }

    @Test
    void isAvroGeneratedType_whenTypeDeclaresSchemaField_thenTrue() {
        TypeElement typeElement = mock(TypeElement.class);
        doReturn(List.of(field("SCHEMA$"))).when(typeElement).getEnclosedElements();

        assertThat(AvroSchemaExtractor.isAvroGeneratedType(typeElement)).isTrue();
    }

    @Test
    void isAvroGeneratedType_whenTypeIsNeitherAnnotatedNorDeclaresSchemaField_thenFalse() {
        TypeElement typeElement = mock(TypeElement.class);
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
        when(processingEnv.getElementUtils().getTypeElement(qualifiedName)).thenReturn(typeElement);
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


    private static Element field(String simpleName) {
        Element element = mock(Element.class);
        Name name = mock(Name.class);
        when(name.toString()).thenReturn(simpleName);
        when(element.getSimpleName()).thenReturn(name);
        when(element.getKind()).thenReturn(ElementKind.FIELD);
        return element;
    }

    @SuppressWarnings("unused")
    public static class MessageTypeWithClassSchema {
        public static final Schema SCHEMA$ = new Schema.Parser().parse(SCHEMA_JSON);

        public static Schema getClassSchema() {
            return SCHEMA$;
        }

        public static class TypeRef {
            public static final String MESSAGE_TYPE_VERSION = "1";
        }
    }

    public static class MessageTypeWithSchemaFieldOnly {
        public static final Schema SCHEMA$ = new Schema.Parser().parse(SCHEMA_JSON);
    }

    public static class FatalMessage {
        public static Object getClassSchema() {
            throw new OutOfMemoryError("fatal fixture");
        }
    }

    public static class ThreadDeathMessage {
        public static Object getClassSchema() {
            throw new ThreadDeath();
        }
    }
}
