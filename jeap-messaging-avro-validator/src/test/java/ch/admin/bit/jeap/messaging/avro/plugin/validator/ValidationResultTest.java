package ch.admin.bit.jeap.messaging.avro.plugin.validator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;


class ValidationResultTest {
    @Test
    void ok() {
        ValidationResult target = ValidationResult.ok();

        Assertions.assertTrue(target.isValid());
        Assertions.assertEquals(Collections.emptyList(), target.getErrors());
    }

    @Test
    void fail() {
        ValidationResult target = ValidationResult.fail("test");

        Assertions.assertFalse(target.isValid());
        Assertions.assertEquals(Collections.singletonList("test"), target.getErrors());
    }

    @Test
    void mergeTrueTrue() {
        ValidationResult res1 = ValidationResult.ok();
        ValidationResult res2 = ValidationResult.ok();

        ValidationResult target = ValidationResult.merge(res1, res2);

        Assertions.assertTrue(target.isValid());
        Assertions.assertEquals(Collections.emptyList(), target.getErrors());
    }

    @Test
    void mergeTrueFalse() {
        ValidationResult res1 = ValidationResult.ok();
        ValidationResult res2 = ValidationResult.fail("test2");

        ValidationResult target = ValidationResult.merge(res1, res2);

        Assertions.assertFalse(target.isValid());
        Assertions.assertEquals(1, target.getErrors().size());
        Assertions.assertTrue(target.getErrors().contains("test2"));
    }

    @Test
    void mergeFalseFalse() {
        ValidationResult res1 = ValidationResult.fail("test1");
        ValidationResult res2 = ValidationResult.fail("test2");

        ValidationResult target = ValidationResult.merge(res1, res2);

        Assertions.assertFalse(target.isValid());
        Assertions.assertEquals(2, target.getErrors().size());
        Assertions.assertTrue(target.getErrors().contains("test1"));
        Assertions.assertTrue(target.getErrors().contains("test2"));
    }
}
