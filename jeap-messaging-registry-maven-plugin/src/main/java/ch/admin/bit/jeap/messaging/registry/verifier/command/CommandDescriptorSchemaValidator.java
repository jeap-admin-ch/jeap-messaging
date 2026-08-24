package ch.admin.bit.jeap.messaging.registry.verifier.command;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidatorUtils;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.io.File;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class CommandDescriptorSchemaValidator {
    private static final Schema SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
            .getSchema(SchemaLocation.of("classpath:CommandDescriptor.schema.json"));
    private final File commandDescriptor;

    static ValidationResult validate(ValidationContext validationContext) {
        CommandDescriptorSchemaValidator commandDescriptorSchemaValidator = new CommandDescriptorSchemaValidator(validationContext.getDescriptorFile());
        return commandDescriptorSchemaValidator.validateSchema();
    }

    private ValidationResult validateSchema() {
        JsonNode commandDescriptorAsJson;
        try {
            commandDescriptorAsJson = ValidatorUtils.loadJson(commandDescriptor);
        } catch (JacksonException e) {
            String message = String.format("Cannot open '%s' as JSON-File: %s",
                    commandDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }

        var errors = SCHEMA.validate(commandDescriptorAsJson);
        if (errors.isEmpty()) {
            return ValidationResult.ok();
        }
        return errors.stream()
                .map(e -> String.format("Command descriptor file '%s' does not correspond to schema: %s",
                        commandDescriptor.getAbsolutePath(), ValidatorUtils.formatSchemaError(e)))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }
}
