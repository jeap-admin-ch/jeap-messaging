package ch.admin.bit.jeap.messaging.registry.verifier.command;

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
class CommandDescriptorSchemaValidator {
    private static final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    private static final String SCHEMA_FILE = "resource:/CommandDescriptor.schema.json";
    private static final JsonSchema schema = getCommandDescriptorSchema();
    private final File commandDescriptor;

    private static JsonSchema getCommandDescriptorSchema() {
        try {
            return factory.getJsonSchema(SCHEMA_FILE);
        } catch (ProcessingException e) {
            throw new RuntimeException("Cannot load command descriptor schema", e);
        }
    }

    static ValidationResult validate(ValidationContext validationContext) {
        CommandDescriptorSchemaValidator commandDescriptorSchemaValidator = new CommandDescriptorSchemaValidator(validationContext.getDescriptorFile());
        return commandDescriptorSchemaValidator.validateSchema();
    }

    private ValidationResult validateSchema() {
        JsonNode commandDescriptorAsJson;
        try {
            commandDescriptorAsJson = JsonLoader.fromFile(commandDescriptor);
        } catch (IOException e) {
            String message = String.format("Cannot open '%s' as JSON-File: %s",
                    commandDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }

        ProcessingReport report;
        try {
            report = schema.validate(commandDescriptorAsJson);
        } catch (ProcessingException e) {
            String message = String.format("Cannot verify schema of '%s': %s",
                    commandDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }

        if (report.isSuccess()) {
            return ValidationResult.ok();
        }
        return StreamSupport.stream(report.spliterator(), false)
                .map(e -> String.format("Command descriptor file '%s' does not correspond to schema: %s",
                        commandDescriptor.getAbsolutePath(), e))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }
}
