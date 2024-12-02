package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class EventDescriptorSchemaValidator {
    private static final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    private static final String SCHEMA_FILE = "resource:/EventDescriptor.schema.json";
    private static final JsonSchema schema = getEventDescriptorSchema();
    private final File eventDescriptor;

    private static JsonSchema getEventDescriptorSchema() {
        try {
            return factory.getJsonSchema(SCHEMA_FILE);
        } catch (ProcessingException e) {
            throw new RuntimeException("Cannot load event descriptor schema", e);
        }
    }

    static ValidationResult validate(ValidationContext validationContext) {
        EventDescriptorSchemaValidator eventValidator = new EventDescriptorSchemaValidator(validationContext.getDescriptorFile());
        return eventValidator.validateSchema();
    }

    private ValidationResult validateSchema() {
        JsonNode eventDescriptorAsJson;
        try {
            eventDescriptorAsJson = JsonLoader.fromFile(eventDescriptor);
        } catch (IOException e) {
            String message = String.format("Cannot open '%s' as JSON-File: %s",
                    eventDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }

        ProcessingReport report;
        try {
            report = schema.validate(eventDescriptorAsJson);
        } catch (ProcessingException e) {
            String message = String.format("Cannot verify schema of '%s': %s",
                    eventDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }

        if (report.isSuccess()) {
            return ValidationResult.ok();
        }
        return StreamSupport.stream(report.spliterator(), false)
                .map(e -> String.format("Event descriptor file '%s' does not correspond to schema: %s",
                        eventDescriptor.getAbsolutePath(), e))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }
}
