package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefiningSystemValidator {
    public static ValidationResult validate(ValidationContext validationContext, JsonNode messageTypeDescriptorJson) {
        String systemName = validationContext.getSystemName();

        String definingSystem = getDefiningSystemOrFallback(messageTypeDescriptorJson);
        String capitalizedSystemName = systemName.toUpperCase();
        if (capitalizedSystemName.equals(definingSystem)) {
            return ValidationResult.ok();
        }
        String message = String.format("Message '%s' has an publishing system '%s' that does not correspond to the capitalized system folder name '%s'",
                validationContext.getMessageTypeName(),
                definingSystem,
                capitalizedSystemName);
        return ValidationResult.fail(message);
    }

    public static String getDefiningSystemOrFallback(JsonNode messageTypeDescriptorJson) {

        JsonNode definingSystemNode = messageTypeDescriptorJson.get("definingSystem");
        if (definingSystemNode != null) {
            return definingSystemNode.asText();
        }
        //One of both must be given
        JsonNode publishingSystem = messageTypeDescriptorJson.get("publishingSystem");
        return publishingSystem == null ? null : publishingSystem.asText();
    }
}
