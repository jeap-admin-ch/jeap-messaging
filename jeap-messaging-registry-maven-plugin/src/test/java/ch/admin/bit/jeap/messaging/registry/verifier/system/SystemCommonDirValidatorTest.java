package ch.admin.bit.jeap.messaging.registry.verifier.system;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

class SystemCommonDirValidatorTest {
    private static ValidationContext validationContext(File systemDir) {
        return ValidationContext.builder()
                .importClassLoader(new ImportClassLoader())
                .systemDir(systemDir)
                .build();
    }

    @Test
    void noIdlFile(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), "Invalid Data");

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        Assertions.assertFalse(result.isValid(), "Common dir contained no IDL file, this is not allowed");
    }

    @Test
    void valid(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), "@namespace(\"test\")\n" +
                "protocol TestProtocol {\n" +
                "  record Test {\n" +
                "    string id;\n" +
                "  }\n" +
                "}\n");

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        Assertions.assertTrue(result.isValid(), String.join(",", result.getErrors()));
    }

    @Test
    void wrongRecordName(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), "@namespace(\"test\")\n" +
                "protocol TestProtocol {\n" +
                "  record TestWrong {\n" +
                "    string id;\n" +
                "  }\n" +
                "}\n");

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        Assertions.assertFalse(result.isValid(), "Common dir contained a file with a record with a wrong name, this is not allowed");
    }

    @Test
    void wrongProtocolName(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), "@namespace(\"test\")\n" +
                "protocol TestWrongProtocol {\n" +
                "  record Test {\n" +
                "    string id;\n" +
                "  }\n" +
                "}\n");

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        Assertions.assertFalse(result.isValid(), "Common dir contained a file with a protocol with a wrong name, this is not allowed");
    }

    @Test
    void multipleRecordsPerFile(@TempDir File tmpDir) throws IOException {
        File commonSystemDir = new File(tmpDir, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        FileUtils.forceMkdir(commonSystemDir);
        FileUtils.write(new File(commonSystemDir, "test.Test.avdl"), "@namespace(\"test\")\n" +
                "protocol TestProtocol {\n" +
                "  record Test1 {\n" +
                "    string id;\n" +
                "  }\n" +
                "  record Test2 {\n" +
                "    string id;\n" +
                "  }\n" +
                "}\n");

        ValidationResult result = SystemCommonDirValidator.validate(validationContext(tmpDir));

        Assertions.assertFalse(result.isValid(), "Common dir contained a file with multiple records, this is not allowed");
    }
}
