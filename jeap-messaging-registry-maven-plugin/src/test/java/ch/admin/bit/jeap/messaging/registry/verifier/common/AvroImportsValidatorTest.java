package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

class AvroImportsValidatorTest {

    private final AvroImportsValidator validator = new AvroImportsValidator(new SystemStreamLog(), true);

    @Test
    void checkUnusedImports_resultSuccessful_whenUsedImport() throws Exception {
        File file = new File("src/test/resources/unittest/UnusedImportTestUsedSimpleName.avdl");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertTrue(result.isValid());
    }

    @Test
    void checkUnusedImports_resultSuccessful_whenUsedImportFullyQualifiedName() throws Exception {
        File file = new File("src/test/resources/unittest/UnusedImportTestUsedFullyQualifiedName.avdl");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertTrue(result.isValid());
    }

    @Test
    void checkUnusedImports_resultSuccessful_whenUsedImportInAUnion() throws Exception {
        File file = new File("src/test/resources/unittest/UnusedImportTestUsedInUnion.avdl");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertTrue(result.isValid());
    }

    @Test
    void checkUnusedImports_resultNotSuccessful_whenUnusedImport() throws Exception {
        File file = new File("src/test/resources/unittest/UnusedImportTestUnused.avdl");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertFalse(result.isValid());
        assertThat(result.getErrors().toString(), containsString("Unused import"));
        assertThat(result.getErrors().toString(), containsString("UnusedImportTestUsedInUnion.avdl"));
    }

    @Test
    void checkUnusedImports_resultIsValid_whenAvscFile() throws Exception {
        File file = new File("Dummy.avsc");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertTrue(result.isValid());
    }

    @Test
    void checkUnusedImports_resultIsValid_whenAvprFile() throws Exception {
        File file = new File("Dummy.avpr");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertTrue(result.isValid());
    }

    @Test
    void checkUnusedImports_resultIsValid_whenTxtrFile() throws Exception {
        File file = new File("Dummy.txt");

        ValidationResult result = validator.validate(file, new ImportClassLoader(new File("src/test/resources/unittest/")));
        assertTrue(result.isValid());
    }
}