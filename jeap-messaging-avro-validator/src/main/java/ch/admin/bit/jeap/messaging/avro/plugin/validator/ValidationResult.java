package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The result of a validation from {@link SchemaValidator}. As we do not only want to have the result
 * but also all the error messages we need to collect the error messages here. To use stream processing to do so
 * we provide some merge methods etc. here as well.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationResult {
    private static final ValidationResult OK = new ValidationResult(true, Collections.emptyList());
    private final boolean valid;
    private final List<String> errors;

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult fail(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }

    public static ValidationResult merge(ValidationResult... res) {
        return Arrays.stream(res)
                .reduce(ok(), ValidationResult::merge);
    }

    private static ValidationResult merge(ValidationResult res1, ValidationResult res2) {
        List<String> errors = Stream.concat(
                res1.getErrors().stream(),
                res2.getErrors().stream()
        ).collect(Collectors.toList());
        boolean valid = res1.isValid() && res2.isValid();
        return new ValidationResult(valid, errors);
    }
}
