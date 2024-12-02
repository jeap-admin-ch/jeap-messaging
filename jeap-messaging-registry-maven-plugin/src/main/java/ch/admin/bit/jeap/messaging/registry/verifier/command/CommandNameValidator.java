package ch.admin.bit.jeap.messaging.registry.verifier.command;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidatorUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class CommandNameValidator {
    private final String systemName;
    private final String commandName;
    private final String commandDescriptorPath;
    private final JsonNode commandDescriptorJson;

    static ValidationResult validate(ValidationContext validationContext, JsonNode commandDescriptorJson) {
        String systemName = validationContext.getSystemName();
        String commandName = validationContext.getMessageTypeName();
        String commandDescriptorPath = validationContext.getDescriptorFile().getAbsolutePath();
        CommandNameValidator commandNameValidator = new CommandNameValidator(systemName, commandName, commandDescriptorPath, commandDescriptorJson);
        return ValidationResult.merge(
                commandNameValidator.commandNameEndsWithCommand(),
                commandNameValidator.commandNameStartsWithSystem(),
                commandNameValidator.commandNoSpecialChars(),
                commandNameValidator.commandNameInFile());
    }

    private ValidationResult commandNameStartsWithSystem() {
        String systemNameCamelCase = ValidatorUtils.getSystemNamePrefix(systemName);
        if (commandName.startsWith(systemNameCamelCase)) {
            return ValidationResult.ok();
        }
        if (systemName.equalsIgnoreCase(ValidatorUtils.JEAP_SYSTEM_NAME)){
            //ignore prefix for system jeap
            return ValidationResult.ok();
        }
        String message = String.format("Name of command descriptor '%s' must start with system name '%s'",
                commandDescriptorPath, systemNameCamelCase);
        return ValidationResult.fail(message);
    }

    private ValidationResult commandNameEndsWithCommand() {
        if (commandName.endsWith("Command")) {
            return ValidationResult.ok();
        }
        String message = String.format("Name of command descriptor '%s' must end with 'Command'",
                commandDescriptorPath);
        return ValidationResult.fail(message);
    }

    private ValidationResult commandNoSpecialChars() {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(commandName);
        if (matcher.matches()) {
            return ValidationResult.ok();
        }
        String message = String.format("Name of command descriptor '%s' must only contain characters [a-zA-Z0-9]",
                commandDescriptorPath);
        return ValidationResult.fail(message);
    }

    private ValidationResult commandNameInFile() {
        JsonNode jsonNode = commandDescriptorJson.get("commandName");
        if (jsonNode == null) {
            return ValidationResult.ok();
        }
        String asText = jsonNode.asText();
        if (commandName.equals(asText)) {
            return ValidationResult.ok();
        }
        String message = String.format("Command name '%s' in command descriptor '%s' does not correspond to file name",
                asText, commandDescriptorPath);
        return ValidationResult.fail(message);
    }
}
