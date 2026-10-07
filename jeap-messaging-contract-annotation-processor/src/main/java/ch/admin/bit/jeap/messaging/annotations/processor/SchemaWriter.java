package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.processor.util.AvroSchemaExtractor;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import static javax.tools.Diagnostic.Kind.NOTE;

/**
 * Writes the avro schema of a message type as a file next to the message contract files. The schema is uploaded
 * together with the contracts by the jeap-messaging-contract-maven-plugin.
 */
class SchemaWriter {

    static final String GENERATION_ENABLED = "messageContractSchemaGenerationEnabled";

    // Weak keys also release aborted compilations. Values never retain the environment or its elements.
    // A null value remembers an unsuccessful extraction so another contract does not repeat it.
    private static final Map<ProcessingEnvironment, Map<String, String>> COMPILATIONS = new WeakHashMap<>();

    static synchronized void processingOver(ProcessingEnvironment env) {
        COMPILATIONS.remove(env);
    }

    private SchemaWriter() {
    }

    static String generateSchema(ProcessingEnvironment processingEnv, Element annotatedElement, TypeElement typeRefElement,
                                 Contract contract) {
        if ("false".equalsIgnoreCase(processingEnv.getOptions().get(GENERATION_ENABLED))) {
            return null;
        }
        String messageTypeName = contract.getMessageTypeName();
        TypeElement messageTypeElement = messageTypeElement(typeRefElement);
        if (messageTypeElement == null) {
            // The TypeRef is not declared in an avro generated message type class, i.e. there is no schema to write.
            // This is not an error: contracts can reference hand-written MessageTypeMetadata implementations.
            processingEnv.getMessager().printMessage(NOTE,
                    "No avro schema file generated for the message type " + messageTypeName +
                    ": no avro generated class declaring a SCHEMA$ field found", annotatedElement);
            return null;
        }

        String fileName = SchemaResources.fileName(processingEnv.getElementUtils().getBinaryName(messageTypeElement).toString(), contract);
        if (alreadyAttempted(processingEnv, fileName)) {
            return schemaAvailable(processingEnv, fileName) ? fileName : null;
        }
        Optional<String> schemaJson = AvroSchemaExtractor.extractSchemaJson(processingEnv, messageTypeElement);
        if (schemaJson.isEmpty()) {
            rememberFailure(processingEnv, fileName);
            processingEnv.getMessager().printMessage(NOTE,
                    "No avro schema file generated for the message type " + messageTypeName +
                    " (schema extraction unavailable; see diagnostics above). " +
                    "The message contract service will read the schema from the message type registry instead.",
                    annotatedElement);
            return null;
        }

        writeSchemaFile(processingEnv, annotatedElement, fileName, schemaJson.get());
        return fileName;
    }

    private static synchronized boolean alreadyAttempted(ProcessingEnvironment env, String fileName) {
        Map<String, String> written = COMPILATIONS.get(env);
        return written != null && written.containsKey(fileName);
    }

    private static synchronized boolean schemaAvailable(ProcessingEnvironment env, String fileName) {
        return COMPILATIONS.get(env).get(fileName) != null;
    }

    private static synchronized void rememberFailure(ProcessingEnvironment env, String fileName) {
        COMPILATIONS.computeIfAbsent(env, ignored -> new HashMap<>()).put(fileName, null);
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

    static synchronized void writeSchemaFile(ProcessingEnvironment processingEnv, Element annotatedElement, String fileName, String schemaJson) {
        Map<String, String> written = COMPILATIONS.computeIfAbsent(processingEnv, ignored -> new HashMap<>());
        String previous = written.get(fileName);
        if (previous != null) {
            if (!previous.equals(schemaJson)) {
                throw MessagingAnnotationProcessingException.processingFailed(new IOException("Conflicting schemas for " + fileName));
            }
            return;
        }
        Filer filer = processingEnv.getFiler();
        try {
            FileObject schemaFile = filer.createResource(StandardLocation.CLASS_OUTPUT, ContractWriter.CONTRACTS_PACKAGE, fileName, annotatedElement);
            // Written as UTF-8 explicitly, as the schema is read as UTF-8 by the jeap-messaging-contract-maven-plugin
            try (Writer writer = new OutputStreamWriter(schemaFile.openOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(schemaJson);
            }
            written.put(fileName, schemaJson);
        } catch (IOException ex) {
            throw MessagingAnnotationProcessingException.processingFailed(ex);
        }
    }
}
