package ch.admin.bit.jeap.messaging.avro.plugin.compiler;

import lombok.*;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;
import org.apache.avro.generic.GenericData;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Compiler for AVRO files. Basically a wrapper for {@link SpecificCompiler}, use to preconfigure this compiler
 * with some shared settings
 */
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AvroCompiler {
    private static final Path MESSAGING_PACKAGE_PATH = Path.of("ch", "admin", "bit", "jeap", "messaging", "avro");
    private static final Path DOMAINEVENT_PACKAGE_PATH = Path.of("ch", "admin", "bit", "jeap", "domainevent", "avro");
    private static final Set<Path> JEAP_MESSAGING_FILES = Set.of(
            DOMAINEVENT_PACKAGE_PATH.resolve("AvroDomainEventType.java"),
            DOMAINEVENT_PACKAGE_PATH.resolve("AvroDomainEventIdentity.java"),
            DOMAINEVENT_PACKAGE_PATH.resolve("AvroDomainEventPublisher.java"),
            DOMAINEVENT_PACKAGE_PATH.resolve("AvroDomainEventUser.java"),
            MESSAGING_PACKAGE_PATH.resolve("AvroMessageType.java"),
            MESSAGING_PACKAGE_PATH.resolve("AvroMessageIdentity.java"),
            MESSAGING_PACKAGE_PATH.resolve("AvroMessagePublisher.java"),
            MESSAGING_PACKAGE_PATH.resolve("AvroMessageUser.java"));
    private static final String TEMPLATE_DIRECTORY = "/velocity-templates/";

    private final String sourceEncoding;
    @Getter
    private final File outputDirectory;
    @Singular
    private final List<Object> additionalTools;

    private final boolean enableDecimalLogicalType;

    /**
     * Compile an Avro-Schema. Generates .class files for the schema and writes them into the outputDirectory
     *
     * @param schema            The schema to compile
     * @param onlyIfFileChanged Recompile only if this file has changed. Can be null, in this case recompile always.
     * @throws IOException If onlyIfFileChanged cannot be read
     */
    public void compileSchema(Schema schema, File onlyIfFileChanged) throws IOException {
        SpecificCompiler compiler = new SpecificCompiler(schema);
        configureCompiler(compiler);
        compiler.compileToDestination(onlyIfFileChanged, outputDirectory);
    }

    /**
     * Compile an Avro-Schema. Generates .class files for the schema and writes them into the outputDirectory
     *
     * @param protocol          The protocol to compile
     * @param onlyIfFileChanged Recompile only if this file has changed. Can be null, in this case recompile always.
     * @throws IOException If onlyIfFileChanged cannot be read
     */
    public void compileProtocol(Protocol protocol, File onlyIfFileChanged) throws IOException {
        SpecificCompiler compiler = new SpecificCompiler(protocol);
        configureCompiler(compiler);
        compiler.compileToDestination(onlyIfFileChanged, outputDirectory);
    }

    private void configureCompiler(SpecificCompiler compiler) {
        compiler.setStringType(GenericData.StringType.String);
        compiler.setTemplateDir(TEMPLATE_DIRECTORY);
        compiler.setFieldVisibility(SpecificCompiler.FieldVisibility.PRIVATE);
        compiler.setCreateOptionalGetters(true);
        compiler.setGettersReturnOptional(false);
        compiler.setCreateSetters(true);
        compiler.setAdditionalVelocityTools(additionalTools);
        compiler.setEnableDecimalLogicalType(enableDecimalLogicalType);
        compiler.setOutputCharacterEncoding(sourceEncoding);
    }

    /**
     * Delete all base files generated in the outputDirectory. This is used as we import
     * those classes from the base library already
     *
     * @throws MojoExecutionException When a file cannot be deleted
     */
    public void deleteBaseEventFiles() throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            return;
        }

        try (Stream<Path> stream = Files.walk(outputDirectory.toPath(), Integer.MAX_VALUE)) {
            List<Path> allFiles = stream
                    .filter(Files::isRegularFile)
                    .toList();

            for (Path current : allFiles) {
                for (Path jeapFile : JEAP_MESSAGING_FILES) {
                    if (current.endsWith(jeapFile)){
                        Files.deleteIfExists(current);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot delete base event files", e);
        }
    }
}
