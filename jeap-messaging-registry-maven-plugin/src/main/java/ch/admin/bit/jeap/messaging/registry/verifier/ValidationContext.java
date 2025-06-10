package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import lombok.Builder;
import lombok.Value;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

@Value
@Builder(toBuilder = true)
public class ValidationContext {
    ImportClassLoader importClassLoader;
    File descriptorDir;
    File oldDescriptorDir;
    String systemName;
    File systemDir;
    File messageTypeDirectory;
    File descriptorFile;
    String messageTypeName;
    MessagingType messagingType;
    boolean failOnUnusedImports;
    Log log;
    String trunkBranchName;
}
