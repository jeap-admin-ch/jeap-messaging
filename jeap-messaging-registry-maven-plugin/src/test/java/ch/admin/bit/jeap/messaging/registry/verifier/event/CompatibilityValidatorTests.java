package ch.admin.bit.jeap.messaging.registry.verifier.event;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.RecordCollection;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.dto.CompatibilityMode;
import ch.admin.bit.jeap.messaging.registry.helper.SemanticVersion;
import ch.admin.bit.jeap.messaging.registry.helper.SemanticVersionList;
import ch.admin.bit.jeap.messaging.registry.helper.VersionCompatibility;
import ch.admin.bit.jeap.messaging.registry.helper.VersionCompatibility.Compatibility;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import ch.admin.bit.jeap.messaging.registry.verifier.common.CompatibilityValidator;
import org.apache.avro.Schema;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CompatibilityValidatorTests {
    private static final String eventName = "name";
    private static final Supplier<Schema.Field> optionalField = generateFieldSupplier("optional", true);
    private static final Supplier<Schema.Field> requiredField = generateFieldSupplier("required", false);
    private static final Supplier<Schema.Field> newOptionalField = generateFieldSupplier("newOptional", true);
    private static final Supplier<Schema.Field> newOptionalFieldNowRequired = generateFieldSupplier("newOptional", false);
    private static final RecordCollection base = generateRecordCollection(optionalField, requiredField);
    private static final RecordCollection compatibleOneWay = generateRecordCollection(optionalField);
    private static final RecordCollection compatibleTwoWay = generateRecordCollection(optionalField, requiredField, newOptionalField);
    private static final RecordCollection compatibleTwoWayTransitive = generateRecordCollection(optionalField, requiredField);
    private static final RecordCollection compatibleTwoWayNonTransitive = generateRecordCollection(optionalField, requiredField, newOptionalFieldNowRequired);
    private static final ValidationContext validationContext = ValidationContext.builder()
            .messageTypeName(eventName)
            .log(mock(Log.class))
            .build();
    private final SemanticVersionList emptyOldSemanticVersionList = new SemanticVersionList(List.of());

    private static RecordCollection generateRecordCollection(Supplier... fieldSupplier) {
        List<Schema.Field> fields = Arrays.stream(fieldSupplier)
                .map(Supplier::get)
                .map(Schema.Field.class::cast)
                .collect(Collectors.toList());
        Schema record = Schema.createRecord(eventName, null, null, false, fields);
        return RecordCollection.of(record);
    }

    private static Supplier<Schema.Field> generateFieldSupplier(String name, boolean optional) {
        if (optional) {
            return () -> new Schema.Field(name, Schema.create(Schema.Type.STRING), null, "default");
        }
        return () -> new Schema.Field(name, Schema.create(Schema.Type.STRING), null);
    }

    private static VersionCompatibility createVersionCompatibility(String newVersion, String oldVersion, CompatibilityMode mode) {
        return new VersionCompatibility(List.of(
                new Compatibility(SemanticVersion.parse(newVersion), oldVersion == null ? null : SemanticVersion.parse(oldVersion), mode)));
    }

    @Test
    void compatibilityModeNode() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base, "1.1.0", compatibleTwoWayNonTransitive);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.NONE);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void compatibilityModeBackwardsFail() {
        Map<String, RecordCollection> records = Map.of("1.0.0", compatibleOneWay, "1.1.0", base);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.BACKWARD);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertFalse(result.isValid(), "Record collection is not backwards compatible");
    }

    @Test
    void compatibilityModeBackwardsSuccess() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base, "1.1.0", compatibleOneWay);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.BACKWARD);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void compatibilityModeForwardsFail() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base, "1.1.0", compatibleOneWay);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.FORWARD);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertFalse(result.isValid(), "Record collection is not forwards compatible");
    }

    @Test
    void compatibilityModeForwardsSuccess() {
        Map<String, RecordCollection> records = Map.of("1.0.0", compatibleOneWay, "1.1.0", base);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.FORWARD);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }


    @Test
    void compatibilityModeFullFail() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base, "1.1.0", compatibleOneWay);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.FULL);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertFalse(result.isValid(), "Record collection is not full compatible");
    }

    @Test
    void compatibilityModeFullSuccess() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base, "1.1.0", compatibleTwoWay);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.FULL);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void missingCompatibleVersionAttribute() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base, "1.1.0", compatibleTwoWay);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0", "1.1.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", "1.0.0", CompatibilityMode.FULL);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void singleVersion_resultOk() {
        Map<String, RecordCollection> records = Map.of("1.0.0", base);
        SemanticVersionList semanticVersionList = SemanticVersionList.from(List.of("1.0.0"));
        VersionCompatibility versionCompatibility = createVersionCompatibility("1.1.0", null, CompatibilityMode.NONE);

        ValidationResult result = CompatibilityValidator.validate(validationContext, versionCompatibility, semanticVersionList, emptyOldSemanticVersionList, records);
        assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }
}
