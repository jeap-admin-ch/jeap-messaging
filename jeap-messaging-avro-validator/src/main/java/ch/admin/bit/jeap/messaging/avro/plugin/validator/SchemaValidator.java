package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import lombok.experimental.UtilityClass;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The schema validator can validate a schema given as {@link RecordCollection}.
 * Here all the rules of what a schema has to look like needs to be implemented.
 */
@UtilityClass
public class SchemaValidator {
    private static final String AVRO_EVENT_NS = "ch.admin.bit.jeap.domainevent.avro";
    private static final String AVRO_COMMON_NS = "ch.admin.bit.jeap.messaging.avro";

    private static final RecordValidator EVENT_VALIDATOR = RecordValidator.create()
            .required("type").type("AvroDomainEventType").namespace(AVRO_EVENT_NS).end()
            .required("identity").type("AvroDomainEventIdentity").namespace(AVRO_EVENT_NS).end()
            .required("publisher").type("AvroDomainEventPublisher").namespace(AVRO_EVENT_NS).end()
            .required("domainEventVersion").type("string").end()
            .optional("references").typeSuffix("References").end()
            .optional("payload").typeSuffix("Payload").end()
            .optional("processId").type("string").end()
            .optional("user").type("AvroDomainEventUser").end()
            .noOtherFields();

    private static final RecordValidator COMMAND_VALIDATOR = RecordValidator.create()
            .required("type").type("AvroMessageType").namespace(AVRO_COMMON_NS).end()
            .required("identity").type("AvroMessageIdentity").namespace(AVRO_COMMON_NS).end()
            .required("publisher").type("AvroMessagePublisher").namespace(AVRO_COMMON_NS).end()
            .required("commandVersion").type("string").end()
            .optional("references").typeSuffix("References").end()
            .optional("payload").typeSuffix("Payload").end()
            .optional("processId").type("string").end()
            .optional("user").type("AvroMessageUser").end()
            .noOtherFields();

    private static final RecordValidator TYPE_VALIDATOR = RecordValidator.create()
            .required("name").type("string").end()
            .required("version").type("string").end()
            .optional("variant").type("string").end()
            .noOtherFields();

    private static final RecordValidator EVENT_IDENTITY_VALIDATOR = RecordValidator.create()
            .required("eventId").type("string").end()
            .required("idempotenceId").type("string").end()
            .required("created").type("long").logicalType(LogicalTypes.timestampMillis()).end()
            .noOtherFields();

    private static final RecordValidator MESSAGE_IDENTITY_VALIDATOR = RecordValidator.create()
            .required("id").type("string").end()
            .required("idempotenceId").type("string").end()
            .required("created").type("long").logicalType(LogicalTypes.timestampMillis()).end()
            .noOtherFields();

    private static final RecordValidator MESSAGE_USER_VALIDATOR = RecordValidator.create()
            .optional("id").type("string").end()
            .optional("familyName").type("string").end()
            .optional("givenName").type("string").end()
            .optional("businessPartnerName").type("string").end()
            .optional("businessPartnerId").type("string").end()
            .required("propertiesMap").type("map<string>").end()
            .noOtherFields();

    private static final RecordValidator PUBLISHER_VALIDATOR = RecordValidator.create()
            .required("system").type("string").end()
            .required("service").type("string").end()
            .noOtherFields();

    private static final RecordValidator REFERENCES_VALIDATOR = RecordValidator.create()
            .forAll().typeSuffix("Reference").canBeOptional().canBeArray().end();

    private static final RecordValidator REFERENCE_VALIDATOR = RecordValidator.create()
            .required("type").type("string").end();

    public static ValidationResult validate(RecordCollection collection) {
        //The validation result validates all the records in the collection individually
        //We can do that as those records are normalized
        return collection.getRecords().stream()
                .map((Schema record) -> validateOne(record, collection))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private static ValidationResult validateOne(Schema record, RecordCollection collection) {
        if (record.getName().endsWith("Event")) {
            ValidationResult validationResult = EVENT_VALIDATOR.validate(record);
            if (validationResult.isValid()) {
                return validateMessageStructure(record, collection);
            }
            return validationResult;
        }
        if (record.getName().endsWith("Command")) {
            ValidationResult validationResult = COMMAND_VALIDATOR.validate(record);
            if (validationResult.isValid()) {
                return validateMessageStructure(record, collection);
            }
            return validationResult;
        }

        // Now lets check the base types as well
        if (AVRO_EVENT_NS.equals(record.getNamespace()) || AVRO_COMMON_NS.equals(record.getNamespace())) {
            switch (record.getName()) {
                case "AvroDomainEventType", "AvroMessageType":
                    return TYPE_VALIDATOR.validate(record);
                case "AvroDomainEventIdentity":
                    return EVENT_IDENTITY_VALIDATOR.validate(record);
                case "AvroMessageIdentity":
                    return MESSAGE_IDENTITY_VALIDATOR.validate(record);
                case "AvroDomainEventPublisher", "AvroMessagePublisher":
                    return PUBLISHER_VALIDATOR.validate(record);
                case "AvroDomainEventUser", "AvroMessageUser":
                    return MESSAGE_USER_VALIDATOR.validate(record);
                default:
                    String message = String.format("You should not use namespace '%s' except for base types. " +
                            "But '%s' is not a base type!", record.getNamespace(), record.getName());
                    return ValidationResult.fail(message);
            }
        }

        return ValidationResult.ok();
    }

    private static ValidationResult validateMessageStructure(Schema record, RecordCollection collection) {
        return resolveMessageReferencesInternal(record, collection)
                .map(schema -> validateMessageReferences(schema, collection))
                .orElse(ValidationResult.ok());
    }

    private static ValidationResult validateMessageReferences(Schema record, RecordCollection collection) {
        if (!record.getName().endsWith("References")) {
            ValidationResult.fail(String.format("Record Type of 'references' field must end with 'References'. Found: '%s'", record.getName()));
        }
        ValidationResult result = REFERENCES_VALIDATOR.validate(record);
        if (result.isValid()) {
            return resolveMessageReferenceList(record, collection).stream()
                    .map(REFERENCE_VALIDATOR::validate)
                    .reduce(ValidationResult.ok(), ValidationResult::merge);
        }
        return result;
    }

    private static Optional<Schema> resolveMessageReferencesInternal(Schema record, RecordCollection collection) {
        Optional<String> referencesRecordName = Optional.ofNullable(record.getField("references"))
                .map(SchemaValidator::getRecordName);
        return referencesRecordName.flatMap(name -> collection.getRecords().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst());
    }

    private static Set<Schema> resolveMessageReferenceList(Schema record, RecordCollection collection) {
        Set<String> messageReferenceNameSet = record.getFields().stream()
                .filter(field -> getRecordName(field).toLowerCase().endsWith("reference"))
                .map(SchemaValidator::getRecordName)
                .collect(Collectors.toSet());
        return collection.getRecords().stream()
                .filter(r -> messageReferenceNameSet.contains(r.getName()))
                .collect(Collectors.toSet());
    }

    private static String getRecordName(Schema.Field field) {
        if (field.schema().getType().equals(Schema.Type.ARRAY)) {
            return field.schema().getElementType().getName();
        }
        return field.schema().getName();
    }

}
