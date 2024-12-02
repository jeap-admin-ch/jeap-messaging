package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidatorUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class EventNameValidator {
    private final String systemName;
    private final String eventName;
    private final String eventDescriptorPath;
    private final JsonNode eventDescriptorJson;

    static ValidationResult validate(ValidationContext validationContext, JsonNode eventDescriptorJson) {
        String systemName = validationContext.getSystemName();
        String eventName = validationContext.getMessageTypeName();
        String eventDescriptorPath = validationContext.getDescriptorFile().getAbsolutePath();
        EventNameValidator eventNameValidator = new EventNameValidator(systemName, eventName, eventDescriptorPath, eventDescriptorJson);
        return ValidationResult.merge(
                eventNameValidator.eventNameEndsWithEvent(),
                eventNameValidator.eventNameStartsWithSystem(),
                eventNameValidator.eventNoSpecialChars(),
                eventNameValidator.eventNameInFile());
    }

    private ValidationResult eventNameStartsWithSystem() {
        String systemNameCamelCase = ValidatorUtils.getSystemNamePrefix(systemName);
        if (eventName.startsWith(systemNameCamelCase)) {
            return ValidationResult.ok();
        }
        if (systemName.equalsIgnoreCase(ValidatorUtils.JEAP_SYSTEM_NAME)){
            //ignore prefix for system jeap
            return ValidationResult.ok();
        }
        String message = String.format("Name of event descriptor '%s' must start with system name '%s'",
                eventDescriptorPath, systemNameCamelCase);
        return ValidationResult.fail(message);
    }

    private ValidationResult eventNameEndsWithEvent() {
        if (eventName.endsWith("Event")) {
            return ValidationResult.ok();
        }
        String message = String.format("Name of event descriptor '%s' must end with 'Event'",
                eventDescriptorPath);
        return ValidationResult.fail(message);
    }

    private ValidationResult eventNoSpecialChars() {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]*");
        Matcher matcher = pattern.matcher(eventName);
        if (matcher.matches()) {
            return ValidationResult.ok();
        }
        String message = String.format("Name of event descriptor '%s' must only contain characters [a-zA-Z0-9]",
                eventDescriptorPath);
        return ValidationResult.fail(message);
    }

    private ValidationResult eventNameInFile() {
        JsonNode jsonNode = eventDescriptorJson.get("eventName");
        if (jsonNode == null) {
            return ValidationResult.ok();
        }
        String asText = jsonNode.asText();
        if (eventName.equals(asText)) {
            return ValidationResult.ok();
        }
        String message = String.format("Event name '%s' in event descriptor '%s' does not correspond to file name",
                asText, eventDescriptorPath);
        return ValidationResult.fail(message);
    }
}
