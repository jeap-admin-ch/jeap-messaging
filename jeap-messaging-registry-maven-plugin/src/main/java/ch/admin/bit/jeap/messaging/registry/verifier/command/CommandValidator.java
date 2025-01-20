package ch.admin.bit.jeap.messaging.registry.verifier.command;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.FileNotChangedValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidatorUtils;
import ch.admin.bit.jeap.messaging.registry.verifier.common.AvroImportsValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.common.AvroSchemaValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.common.DefiningSystemValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.common.NoDanglingSchemaValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandValidator {
    public static ValidationResult validate(ValidationContext validationContext) {
        JsonNode commandDescriptorJson;
        Optional<JsonNode> oldCommandDescriptorJson;
        try {
            commandDescriptorJson = JsonLoader.fromFile(validationContext.getDescriptorFile());
            oldCommandDescriptorJson = ValidatorUtils.loadOldDescriptorIfExists(validationContext);
        } catch (IOException e) {
            String message = String.format("File is not a valid JSON-File: %s", e.getMessage());
            return ValidationResult.fail(message);
        }
        ValidationResult validationResult = CommandDescriptorSchemaValidator.validate(validationContext);
        if (!validationResult.isValid()) {
            //If the file format is not valid, do not even try to check other aspects
            return validationResult;
        }
        return ValidationResult.merge(
                CommandNameValidator.validate(validationContext, commandDescriptorJson),
                DefiningSystemValidator.validate(validationContext, commandDescriptorJson),
                AvroImportsValidator.validate(validationContext),
                AvroSchemaValidator.validate(validationContext, commandDescriptorJson, oldCommandDescriptorJson),
                FileNotChangedValidator.noExistingSchemasChanged(validationContext),
                NoDanglingSchemaValidator.validate(validationContext.getMessageTypeDirectory(), commandDescriptorJson)
        );
    }
}
