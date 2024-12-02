package ch.admin.bit.jeap.messaging.avro.plugin.mojo;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.AvroCompiler;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.interfaces.InterfaceTool;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.RecordCollection;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.SchemaValidator;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;
import java.io.IOException;

/**
 * Base for various Avro Compiler Mojo.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractAvroMojo extends AbstractMojo {
    @Getter(AccessLevel.PROTECTED)
    @Parameter(name = "sourceDirectory", defaultValue = "${basedir}/src/main/avro")
    @SuppressWarnings("unused")
    private File sourceDirectory;
    @Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources")
    @SuppressWarnings("unused")
    private File outputDirectory;
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    @SuppressWarnings("unused")
    private MavenProject project;
    @Parameter(name = "deleteBaseEventFiles", defaultValue = "true")
    @SuppressWarnings("CanBeFinal")
    private boolean deleteBaseEventFiles = true;
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "false")
    @SuppressWarnings("unused")
    private boolean enableDecimalLogicalType;

    private ValidationResult overallResult = ValidationResult.ok();

    @Override
    public void execute() throws MojoExecutionException {
        ensureDirectoryExists(sourceDirectory);
        String sourceEncoding = project.getProperties().getProperty("project.build.sourceEncoding");
        InterfaceTool interfaceTool = new InterfaceTool();
        AvroCompiler avroCompiler = AvroCompiler.builder()
                .sourceEncoding(sourceEncoding)
                .outputDirectory(outputDirectory)
                .additionalTool(interfaceTool)
                .enableDecimalLogicalType(enableDecimalLogicalType)
                .build();
        for (String filename : getIncludedFiles()) {
            File file = new File(sourceDirectory, filename);
            compile(avroCompiler, file);
        }

        if (deleteBaseEventFiles) {
            avroCompiler.deleteBaseEventFiles();
        }

        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

        if (!overallResult.isValid()) {
            throw new MojoExecutionException("At least one avro schema is not a valid message. Check log for details");
        }
    }

    private void ensureDirectoryExists(File sourceDirectory) throws MojoExecutionException {
        boolean hasSourceDir = null != sourceDirectory && sourceDirectory.isDirectory();
        if (!hasSourceDir) {
            throw new MojoExecutionException("sourceDirectory: " + sourceDirectory + " is a directory");
        }
    }

    private String[] getIncludedFiles() {
        FileSetManager fileSetManager = new FileSetManager();
        FileSet fs = new FileSet();
        fs.setDirectory(sourceDirectory.getAbsolutePath());
        fs.setFollowSymlinks(false);

        for (String include : getIncludedFileEndings()) {
            fs.addInclude("**/*." + include);
        }
        return fileSetManager.getIncludedFiles(fs);
    }

    protected abstract String[] getIncludedFileEndings();

    protected abstract void compile(AvroCompiler avroCompiler, File file) throws MojoExecutionException;

    void compileIdl(AvroCompiler avroCompiler, File file, ImportClassLoader importClassLoader) throws MojoExecutionException {
        try {
            IdlFileParser idlFileParser = new IdlFileParser(importClassLoader);
            Protocol protocol = idlFileParser.parseIdlFile(file);
            if (isValid(RecordCollection.of(protocol), file)) {
                //We cannot skip compile when the file has not changed
                //as there might be includes which have changed
                getLog().debug("Compile protocol " + protocol.getName() + "from IDL file " + file.getAbsolutePath());
                protocol.getTypes().forEach(t -> getLog().debug("Type " + t.getName() + " is in this record"));
                avroCompiler.compileProtocol(protocol, null);
            }
        } catch (IOException | ParseException e) {
            throw compileException(e, file);
        }
    }

    void compileSchema(AvroCompiler avroCompiler, File file) throws MojoExecutionException {
        try {
            Schema.Parser parser = new Schema.Parser();
            Schema schema = parser.parse(file);
            if (isValid(RecordCollection.of(schema), file)) {
                getLog().debug("Compile schema " + schema.getName() + "from schema file " + file.getAbsolutePath());
                avroCompiler.compileSchema(schema, file);
            }
        } catch (IOException e) {
            throw compileException(e, file);
        }
    }

    void compileProtocol(AvroCompiler avroCompiler, File file) throws MojoExecutionException {
        try {
            Protocol protocol = Protocol.parse(file);
            if (isValid(RecordCollection.of(protocol), file)) {
                getLog().debug("Compile protocol " + protocol.getName() + "from protocol file " + file.getAbsolutePath());
                protocol.getTypes().forEach(t -> getLog().debug("Type " + t.getName() + " is in this record"));
                avroCompiler.compileProtocol(protocol, file);
            }
        } catch (IOException e) {
            throw compileException(e, file);
        }
    }

    private MojoExecutionException compileException(Exception e, File file) {
        return new MojoExecutionException("Could not compile file " + file.getAbsolutePath(), e);
    }

    private boolean isValid(RecordCollection recordCollection, File src) {
        ValidationResult result = SchemaValidator.validate(recordCollection);
        if (!result.isValid()) {
            result.getErrors().stream()
                    .map(e -> "In file " + src.getAbsolutePath() + ": " + e)
                    .forEach(getLog()::error);
        }
        overallResult = ValidationResult.merge(result, overallResult);
        return result.isValid();
    }
}
