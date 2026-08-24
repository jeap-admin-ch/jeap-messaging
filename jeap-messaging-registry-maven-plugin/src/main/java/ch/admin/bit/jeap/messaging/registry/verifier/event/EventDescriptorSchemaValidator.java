package ch.admin.bit.jeap.messaging.registry.verifier.event;

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
class EventDescriptorSchemaValidator {
    private static final Schema SCHEMA = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
            .getSchema(SchemaLocation.of("classpath:EventDescriptor.schema.json"));
    private final File eventDescriptor;

    static ValidationResult validate(ValidationContext validationContext) {
        EventDescriptorSchemaValidator eventValidator = new EventDescriptorSchemaValidator(validationContext.getDescriptorFile());
        return eventValidator.validateSchema();
    }

    private ValidationResult validateSchema() {
        JsonNode eventDescriptorAsJson;
        try {
            eventDescriptorAsJson = ValidatorUtils.loadJson(eventDescriptor);
        } catch (JacksonException e) {
            String message = String.format("Cannot open '%s' as JSON-File: %s",
                    eventDescriptor.getAbsolutePath(),
                    e.getMessage());
            return ValidationResult.fail(message);
        }

        var errors = SCHEMA.validate(eventDescriptorAsJson);
        if (errors.isEmpty()) {
            return ValidationResult.ok();
        }
        return errors.stream()
                .map(e -> String.format("Event descriptor file '%s' does not correspond to schema: %s",
                        eventDescriptor.getAbsolutePath(), ValidatorUtils.formatSchemaError(e)))
                .map(ValidationResult::fail)
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }
}
