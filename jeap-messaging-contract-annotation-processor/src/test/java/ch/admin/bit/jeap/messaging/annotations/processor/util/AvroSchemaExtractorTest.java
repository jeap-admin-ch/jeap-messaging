package ch.admin.bit.jeap.messaging.annotations.processor.util;

import com.sun.source.util.Trees;
import org.apache.avro.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AvroSchemaExtractorTest {
    private static final String JSON = "{\"type\":\"record\",\"name\":\"Event\",\"fields\":[]}";
    private final ProcessingEnvironment env = mock(ProcessingEnvironment.class);
    private MockedStatic<Trees> trees;

    @BeforeEach
    void compiler() throws Exception {
        trees = mockStatic(Trees.class);
        trees.when(() -> Trees.instance(env)).thenReturn(mock(Trees.class));
        Elements elements = mock(Elements.class);
        when(env.getElementUtils()).thenReturn(elements);
        when(elements.getBinaryName(any())).thenAnswer(call -> ((TypeElement) call.getArgument(0)).getQualifiedName());
        Filer filer = mock(Filer.class);
        when(env.getFiler()).thenReturn(filer);
        when(filer.getResource(eq(StandardLocation.CLASS_PATH), anyString(), anyString())).thenAnswer(call -> {
            String resource = call.<String>getArgument(1).replace('.', '/') + "/" + call.<String>getArgument(2);
            FileObject file = mock(FileObject.class);
            when(file.openInputStream()).thenAnswer(ignored -> {
                var stream = getClass().getClassLoader().getResourceAsStream(resource);
                if (stream == null) throw new FileNotFoundException(resource);
                return stream;
            });
            return file;
        });
        when(elements.getFileObjectOf(any())).thenAnswer(call -> {
            String name = ((TypeElement) call.getArgument(0)).getQualifiedName().toString();
            int separator = name.lastIndexOf('.');
            JavaFileObject file = mock(JavaFileObject.class);
            when(file.getKind()).thenReturn(JavaFileObject.Kind.CLASS);
            when(file.openInputStream()).thenAnswer(ignored -> filer.getResource(StandardLocation.CLASS_PATH,
                    name.substring(0, separator), name.substring(separator + 1) + ".class").openInputStream());
            return file;
        });
        when(env.getMessager()).thenReturn(mock(Messager.class));
    }

    @AfterEach
    void close() {
        trees.close();
    }

    @Test
    void readsLiteralWithoutExecutingRemainingInitializerOrGetter() {
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(Literal.class))).contains(JSON);
    }

    @Test
    void readsSplitLiteralVarargs() {
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(Split.class))).contains(JSON);
    }

    @Test
    void rejectsOtherMethodsEvenWithJsonArgument() {
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(OtherCall.class))).isEmpty();
    }

    @Test
    void rejectsMalformedOrNonRecordJson() {
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(Invalid.class))).isEmpty();
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(Primitive.class))).isEmpty();
    }

    @Test
    void compilerReportedSourceNeverReadsStaleBinary() {
        TypeElement type = type(Literal.class);
        JavaFileObject source = mock(JavaFileObject.class);
        when(source.getKind()).thenReturn(JavaFileObject.Kind.SOURCE);
        when(env.getElementUtils().getFileObjectOf(type)).thenReturn(source);
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type)).isEmpty();
        verifyNoInteractions(env.getFiler());
    }

    @Test
    void unknownBytecodeBeforeSchemaIsNotExecuted() {
        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(UnknownInitializer.class))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void classfileDiagnosticIncludesWrappedRootCauseAndTerminatesOnCycles(boolean cyclic) throws Exception {
        IOException root = new IOException("Permission denied for message classfile");
        IOException outer = new IOException("Compiler resource access failed", new InvocationTargetException(root));
        if (cyclic) {
            root.initCause(outer);
        }
        Filer filer = env.getFiler();
        doThrow(outer).when(filer).getResource(eq(StandardLocation.CLASS_PATH), anyString(), anyString());

        assertThat(AvroSchemaExtractor.extractSchemaJson(env, type(Literal.class))).isEmpty();

        ArgumentCaptor<CharSequence> diagnostic = ArgumentCaptor.forClass(CharSequence.class);
        verify(env.getMessager()).printMessage(eq(Diagnostic.Kind.NOTE), diagnostic.capture());
        assertThat(diagnostic.getValue().toString())
                .contains("Compiler classfile unavailable", "caused by: java.lang.reflect.InvocationTargetException")
                .containsOnlyOnce("Compiler resource access failed")
                .containsOnlyOnce("Permission denied for message classfile");
    }

    @ParameterizedTest
    @ValueSource(strings = {"hand.avsc", "{bad}", "{\"type\":\"record\",\"name\":\"Event\",\"fields\":[]} garbage",
            "{\"type\":\"record\"}", "\"string\"", "{type:'record',name:'Event',fields:[]}",
            "{\"type\":\"record\",\"name\":\"Event\",\"fields\":[{\"name\":\"a\"}]}"})
    void rejectsInvalidRecordShape(String json) {
        assertThat(SchemaJson.isRecord(json)).isFalse();
    }

    private TypeElement type(Class<?> fixture) {
        TypeElement type = mock(TypeElement.class);
        Name name = mock(Name.class);
        when(name.toString()).thenReturn(fixture.getName());
        when(type.getQualifiedName()).thenReturn(name);
        when(env.getElementUtils().getTypeElement(name)).thenReturn(type);
        return type;
    }

    public static class Literal {
        public static final Schema SCHEMA$ = new Schema.Parser().parse(JSON);
        static { if (System.nanoTime() != 0) throw new AssertionError("must never initialize"); }
        public static Schema getClassSchema() { throw new AssertionError("must never invoke"); }
    }

    public static class Split {
        public static final Schema SCHEMA$ = new Schema.Parser().parse("{\"type\":\"record\",", "\"name\":\"Event\",", "\"fields\":[]}");
    }

    public static class OtherCall {
        public static final Schema SCHEMA$ = load(JSON);
        private static Schema load(String value) { throw new AssertionError(value); }
    }

    public static class Invalid {
        public static final Schema SCHEMA$ = new Schema.Parser().parse("hand.avsc");
    }

    public static class Primitive {
        public static final Schema SCHEMA$ = new Schema.Parser().parse("\"string\"");
    }

    public static class UnknownInitializer {
        static { if (System.nanoTime() != 0) throw new AssertionError("must never initialize"); }
        public static final Schema SCHEMA$ = new Schema.Parser().parse(JSON);
    }
}
