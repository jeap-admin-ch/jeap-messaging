package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.contract.v2.SchemaResources;
import ch.admin.bit.jeap.messaging.annotations.processor.util.AvroSchemaExtractor;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaWriterTest {
    @Test
    void duplicateContractsExtractOnlyOnceAndNewCompilationExtractsAgain() throws Exception {
        ProcessingEnvironment env = mock(ProcessingEnvironment.class);
        Filer filer = mock(Filer.class);
        Elements elements = mock(Elements.class);
        TypeElement typeRef = mock(TypeElement.class);
        TypeElement messageType = mock(TypeElement.class);
        Name binaryName = mock(Name.class);
        Element origin = mock(Element.class);
        FileObject resource = mock(FileObject.class);
        when(env.getFiler()).thenReturn(filer);
        when(env.getElementUtils()).thenReturn(elements);
        when(elements.getBinaryName(messageType)).thenReturn(binaryName);
        when(binaryName.toString()).thenReturn("example.Event");
        when(typeRef.getEnclosingElement()).thenReturn(messageType);
        when(messageType.getKind()).thenReturn(ElementKind.CLASS);
        when(filer.createResource(eq(StandardLocation.CLASS_OUTPUT), eq(ContractWriter.CONTRACTS_PACKAGE), anyString(), eq(origin)))
                .thenReturn(resource);
        when(resource.openOutputStream()).thenAnswer(ignored -> new ByteArrayOutputStream());
        Contract.ContractBuilder builder = Contract.builder().messageTypeName("Event").messageTypeVersion("1");
        try (var extractor = mockStatic(AvroSchemaExtractor.class)) {
            extractor.when(() -> AvroSchemaExtractor.isAvroGeneratedType(messageType)).thenReturn(true);
            extractor.when(() -> AvroSchemaExtractor.extractSchemaJson(env, messageType, typeRef)).thenReturn(Optional.of("schema"));
            String first = SchemaWriter.generateSchema(env, origin, typeRef, builder.role("producer").build());
            assertThat(SchemaWriter.generateSchema(env, origin, typeRef, builder.role("consumer").build())).isEqualTo(first);
            extractor.verify(() -> AvroSchemaExtractor.extractSchemaJson(env, messageType, typeRef), times(1));
            verify(filer, times(1)).createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, first, origin);
            SchemaWriter.processingOver(env);
            SchemaWriter.generateSchema(env, origin, typeRef, builder.build());
            extractor.verify(() -> AvroSchemaExtractor.extractSchemaJson(env, messageType, typeRef), times(2));
        } finally {
            SchemaWriter.processingOver(env);
        }
    }

    @Test
    void duplicateIsSkippedBeforeCreatingResourceAndConflictingContentIsRejected() throws Exception {
        ProcessingEnvironment env = mock(ProcessingEnvironment.class);
        Filer filer = mock(Filer.class);
        Element origin = mock(Element.class);
        FileObject resource = mock(FileObject.class);
        when(env.getFiler()).thenReturn(filer);
        when(filer.createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, "schema.avsc", origin)).thenReturn(resource);
        when(resource.openOutputStream()).thenAnswer(ignored -> new ByteArrayOutputStream());

        SchemaWriter.writeSchemaFile(env, origin, "schema.avsc", "schema");
        SchemaWriter.writeSchemaFile(env, origin, "schema.avsc", "schema");
        assertThatThrownBy(() -> SchemaWriter.writeSchemaFile(env, origin, "schema.avsc", "other"))
                .isInstanceOf(MessagingAnnotationProcessingException.class).hasRootCauseMessage("Conflicting schemas for schema.avsc");
        verify(filer, times(1)).createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, "schema.avsc", origin);

        SchemaWriter.processingOver(env);
        SchemaWriter.writeSchemaFile(env, origin, "schema.avsc", "new compilation");
        verify(filer, times(2)).createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, "schema.avsc", origin);
        SchemaWriter.processingOver(env);
    }

    @Test
    void writeFailuresPropagateAndAreNotRememberedAsSuccessfulWrites() throws Exception {
        ProcessingEnvironment env = mock(ProcessingEnvironment.class);
        Filer filer = mock(Filer.class);
        Element origin = mock(Element.class);
        FileObject resource = mock(FileObject.class);
        when(env.getFiler()).thenReturn(filer);
        when(filer.createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, "schema.avsc", origin)).thenReturn(resource);
        when(resource.openOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("disk full");
            }
        });
        assertThatThrownBy(() -> SchemaWriter.writeSchemaFile(env, origin, "schema.avsc", "schema"))
                .isInstanceOf(MessagingAnnotationProcessingException.class).hasRootCauseMessage("disk full");
        when(resource.openOutputStream()).thenReturn(new ByteArrayOutputStream());
        SchemaWriter.writeSchemaFile(env, origin, "schema.avsc", "schema");
        verify(filer, times(2)).createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, "schema.avsc", origin);
        SchemaWriter.processingOver(env);
    }

    @Test
    void resourceIdentityIncludesClassSystemRegistryAndVersionButNotRoleOrApp() {
        Contract.ContractBuilder builder = Contract.builder().systemName("system").registryUrl("registry")
                .registryBranch("master").registryCommit("commit").messageTypeName("Event").messageTypeVersion("1");
        String original = SchemaResources.fileName("a.Event", builder.build());
        assertThat(SchemaResources.fileName("b.Event", builder.build())).isNotEqualTo(original);
        assertThat(SchemaResources.fileName("a.Event", builder.systemName("other").build())).isNotEqualTo(original);
        assertThat(SchemaResources.fileName("a.Event", builder.systemName("system").registryUrl("other").build())).isNotEqualTo(original);
        assertThat(SchemaResources.fileName("a.Event", builder.registryUrl("registry").messageTypeVersion("2").build())).isNotEqualTo(original);
        assertThat(SchemaResources.fileName("a.Event", builder.messageTypeVersion("1").appName("app").role("consumer").build())).isEqualTo(original);
    }
}
