package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.processor.util.AvroSchemaExtractor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * Writes the avro schema of a message type as a file next to the message contract files. The schema is uploaded
 * together with the contracts by the jeap-messaging-contract-maven-plugin.
 */
class SchemaWriter {

    static final String SCHEMA_SUFFIX = ".avsc";

    private SchemaWriter() {
    }

    static void generateSchema(ProcessingEnvironment processingEnv, Element annotatedElement, TypeElement typeRefElement,
                               String messageTypeName, String messageTypeVersion) {
        TypeElement messageTypeElement = messageTypeElement(typeRefElement);
        if (messageTypeElement == null) {
            // The TypeRef is not declared in an avro generated message type class, i.e. there is no schema to write.
            // This is not an error: contracts can reference hand-written MessageTypeMetadata implementations.
            processingEnv.getMessager().printMessage(NOTE,
                    "No avro schema file generated for the message type " + messageTypeName +
                    ": no avro generated class declaring a SCHEMA$ field found", annotatedElement);
            return;
        }

        Optional<String> schemaJson = AvroSchemaExtractor.extractSchemaJson(processingEnv, messageTypeElement);
        if (schemaJson.isEmpty()) {
            processingEnv.getMessager().printMessage(WARNING,
                    "No avro schema file generated for the message type " + messageTypeName +
                    " (unable to read the field " + messageTypeElement.getQualifiedName() + ".SCHEMA$). " +
                    "The message contract service will read the schema from the message type registry instead.",
                    annotatedElement);
            return;
        }

        writeSchemaFile(processingEnv, annotatedElement, getFileName(messageTypeName, messageTypeVersion), schemaJson.get());
    }

    private static TypeElement messageTypeElement(TypeElement typeRefElement) {
        Element enclosingElement = typeRefElement.getEnclosingElement();
        if (enclosingElement instanceof TypeElement enclosingTypeElement
            && enclosingElement.getKind() == ElementKind.CLASS
            && AvroSchemaExtractor.isAvroGeneratedType(enclosingTypeElement)) {
            return enclosingTypeElement;
        }
        return null;
    }

    private static void writeSchemaFile(ProcessingEnvironment processingEnv, Element annotatedElement, String fileName, String schemaJson) {
        Filer filer = processingEnv.getFiler();
        try {
            FileObject schemaFile = filer.createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, fileName, annotatedElement);
            // Written as UTF-8 explicitly, as the schema is read as UTF-8 by the jeap-messaging-contract-maven-plugin
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(schemaFile.openOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(schemaJson);
            }
        } catch (FilerException ex) {
            // The schema has already been written for this message type version, i.e. by a contract for another role
            // or another app name - one schema file per message type version is sufficient.
            processingEnv.getMessager().printMessage(NOTE, "Schema file " + fileName + " has already been generated");
        } catch (IOException ex) {
            throw MessagingAnnotationProcessingException.processingFailed(ex);
        }
    }

    private static String getFileName(String messageTypeName, String messageTypeVersion) {
        return messageTypeName + "-" + messageTypeVersion + SCHEMA_SUFFIX;
    }
}
