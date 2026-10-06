package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import ch.admin.bit.jeap.messaging.contract.v2.SchemaResources;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaWriterTest {
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
