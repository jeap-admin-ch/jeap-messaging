package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.RecordCollection;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.SemanticVersion;
import ch.admin.bit.jeap.messaging.registry.helper.SemanticVersionList;
import ch.admin.bit.jeap.messaging.registry.helper.VersionCompatibility;
import ch.admin.bit.jeap.messaging.registry.helper.VersionCompatibility.Compatibility;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
@RequiredArgsConstructor
public class CompatibilityValidator {
    private final Map<String, RecordCollection> recordCollections;
    private final SemanticVersionList semanticVersionList;
    private final SemanticVersionList oldSemanticVersionList;
    private final VersionCompatibility versionCompatibility;
    private final String messageTypeName;
    private final Log log;

    public static ValidationResult validate(ValidationContext validationContext, VersionCompatibility versionCompatibility,
                                            SemanticVersionList semanticVersionList, SemanticVersionList oldSemanticVersionList,
                                            Map<String, RecordCollection> recordCollections) {

        return CompatibilityValidator.builder()
                .messageTypeName(validationContext.getMessageTypeName())
                .versionCompatibility(versionCompatibility)
                .semanticVersionList(semanticVersionList)
                .oldSemanticVersionList(oldSemanticVersionList)
                .recordCollections(recordCollections)
                .log(validationContext.getLog())
                .build()
                .validateCompatibility();
    }

    private ValidationResult validateCompatibility() {
        ValidationResult result = ValidationResult.ok();
        List<SemanticVersion> versions = semanticVersionList.getVersions();
        if (versions.size() < 2) {
            // Validating compatibility between versions requires at least two versions to be present
            return ValidationResult.ok();
        }

        for (int i = 1; i < versions.size(); i++) {
            SemanticVersion previousVersion = versions.get(i - 1);
            SemanticVersion version = versions.get(i);

            // Compatibility validation is only executed for new message type versions. Besides a performance optimisation,
            // this mostly due to backwards compatibility with older descriptors that did not yet have a compatibility
            // mode definition on message type versions.
            if (isNewMessageTypeVersion(version, oldSemanticVersionList)) {
                result = ValidationResult.merge(result, validateCompatibilityOfVersions(version, previousVersion));
            }
        }

        return result;
    }

    private static boolean isNewMessageTypeVersion(SemanticVersion version, SemanticVersionList oldSemanticVersionList) {
        return !oldSemanticVersionList.getVersions().contains(version);
    }

    private ValidationResult validateCompatibilityOfVersions(SemanticVersion version, SemanticVersion previousVersion) {
        Optional<Compatibility> compatibilityOptional = versionCompatibility.getCompatibility(version);

        if (compatibilityOptional.isEmpty()) {
            return failDueToMissingCompatibilityMode(version);
        }

        Compatibility compatibility = compatibilityOptional.get();
        SemanticVersion oldVersion = compatibility.oldVersion();
        log.info("Validating compatibility of %s from version %s to version %s (mode: %s)"
                .formatted(messageTypeName, version, oldVersion, compatibility.compatibilityMode()));
        return validateCompatibilityOfSchemas(compatibility);
    }

    private ValidationResult failDueToMissingCompatibilityMode(SemanticVersion newVersion) {
        return ValidationResult.fail("""
                The new message type %s in version %s is missing the mandatory 'compatibilityMode' attribute. Please
                define the compatibility mode:
                {
                  "version": "%s",
                  "compatibilityMode": "BACKWARD|FORWARD|FULL|NONE",
                  "valueSchema": "...",
                  "keySchema": "...",
                  "compatibleVersion": "n.n.n", // Optional, default: Previous version
                }
                """.formatted(messageTypeName, newVersion, newVersion));
    }

    private ValidationResult validateCompatibilityOfSchemas(Compatibility compatibility) {
        SemanticVersion newVersion = compatibility.newVersion();
        SemanticVersion oldVersion = compatibility.oldVersion();
        Schema older = getSchema(oldVersion.toString());
        Schema newer = getSchema(newVersion.toString());
        ValidationResult result = ValidationResult.ok();
        if (compatibility.compatibilityMode().isBackwardOrFull()) {
            result = SchemaCompatibility.checkReaderWriterCompatibility(newer, older)
                    .getResult()
                    .getIncompatibilities()
                    .stream()
                    .map(s -> "Schemas %s version %s and %s are not backward compatible: %s".formatted(
                            messageTypeName, newVersion, oldVersion, s))
                    .map(ValidationResult::fail)
                    .reduce(result, ValidationResult::merge);
        }

        if (compatibility.compatibilityMode().isForwardOrFull()) {
            result = SchemaCompatibility.checkReaderWriterCompatibility(older, newer)
                    .getResult()
                    .getIncompatibilities()
                    .stream()
                    .map(s -> "Schemas %s version %s and %s are not forward compatible: %s".formatted(
                            messageTypeName, newVersion, oldVersion, s))
                    .map(ValidationResult::fail)
                    .reduce(result, ValidationResult::merge);
        }
        return result;
    }

    private Schema getSchema(String version) {
        RecordCollection recordCollection = recordCollections.get(version);
        return recordCollection.getRecords().stream()
                .filter(f -> f.getName().equals(messageTypeName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Schema for message type %s:%s not found in file".formatted(messageTypeName, version)));
    }
}
