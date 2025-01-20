package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;

import java.io.File;

final class ImportClassLoaderHelper {

    static ImportClassLoader generateImportClassLoader(ValidationContext validationContext) {
        File commonRootDir = new File(validationContext.getDescriptorDir(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File commonSystemDir = new File(validationContext.getSystemDir(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
        return new ImportClassLoader(validationContext.getImportClassLoader(), commonRootDir, commonSystemDir);
    }
}
