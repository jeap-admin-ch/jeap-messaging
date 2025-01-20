package ch.admin.bit.jeap.messaging.avro.plugin.mojo;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.AvroCompiler;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.git.GitClient;
import ch.admin.bit.jeap.messaging.avro.plugin.git.GitDiffDto;
import ch.admin.bit.jeap.messaging.avro.plugin.git.NewMessageTypeVersionDto;
import ch.admin.bit.jeap.messaging.avro.plugin.helper.GeneratedSourcesCleaner;
import ch.admin.bit.jeap.messaging.avro.plugin.helper.PomFileGenerator;
import ch.admin.bit.jeap.messaging.avro.plugin.helper.RegistryHelper;
import ch.admin.bit.jeap.messaging.avro.plugin.helper.TypeDescriptorFactory;
import ch.admin.bit.jeap.messaging.avro.plugin.interfaces.InterfaceTool;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeVersion;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata.MessageTypeMetadata;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.metadata.MessageTypeMetadataProvider;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.service.MessageSchema;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.service.TypeReference;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.RecordCollection;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.SchemaValidator;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Mojo(name = "compile-message-types", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class MessageTypesCompilerMojo extends AbstractMojo {

    private final String commonLibVersion;
    private final GeneratedSourcesCleaner generatedSourcesCleaner = new GeneratedSourcesCleaner();
    @Parameter(name = "sourceDirectory", defaultValue = "${basedir}/descriptor")
    @SuppressWarnings("unused")
    private File sourceDirectory;
    @Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources")
    @SuppressWarnings("unused")
    private File outputDirectory;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    @SuppressWarnings("unused")
    private MavenProject project;
    @Parameter(name = "generateAllMessageTypes", defaultValue = "false")
    @Setter
    private boolean generateAllMessageTypes;
    @Parameter(name = "groupIdPrefix", required = true)
    @Setter
    private String groupIdPrefix;
    @Parameter(name = "jeapMessagingVersion", defaultValue = "${jeap-messaging.version}", required = true)
    @SuppressWarnings("unused")
    private String jeapMessagingVersion;
    @Parameter(name = "currentBranch", defaultValue = "${git.branch}", required = true, readonly = true)
    @Setter
    private String currentBranch;
    @Parameter(name = "commitId", defaultValue = "${git.commit.id}", required = true, readonly = true)
    @Setter
    private String commitId;
    @Parameter(name = "gitUrl", required = true, readonly = true)
    @Setter
    private String gitUrl;
    @Parameter(name = "trunkBranchName", defaultValue = "master", required = true)
    @Setter
    private String trunkBranchName;
    @Parameter(name = "skip", defaultValue = "false", required = true)
    @Setter
    private boolean skip;
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "false")
    @SuppressWarnings("unused")
    private boolean enableDecimalLogicalType;
    private Map<String, List<Path>> commonDefinitionsPerSystem = new HashMap<>();
    private ValidationResult overallResult = ValidationResult.ok();

    public MessageTypesCompilerMojo() {
        this.commonLibVersion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss"));
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Message type compilation is skipped");
            return;
        }

        getLog().info("Current Branch: " + this.currentBranch);
        String sourceEncoding = project.getProperties().getProperty("project.build.sourceEncoding");
        AvroCompiler avroCompiler = AvroCompiler.builder()
                .sourceEncoding(sourceEncoding)
                .outputDirectory(outputDirectory)
                .additionalTool(new InterfaceTool())
                .enableDecimalLogicalType(enableDecimalLogicalType)
                .build();

        GitClient gitClient = new GitClient(this.project.getBasedir().getAbsolutePath(), this.gitUrl, this.trunkBranchName);

        if (generateAllMessageTypes) {
            compile(avroCompiler);
        } else {
            final GitDiffDto gitDiff = gitClient.getGitDiff(this.currentBranch);
            if (gitDiff.hasChanges()) {
                getLog().info("New messages types since compared commit: " + newTypes(gitDiff.newMessageTypeVersions()));
                compile(avroCompiler, gitDiff);
            } else {
                getLog().info("No changes from compared commit. Skipping compile...");
            }
        }

        generatedSourcesCleaner.cleanupDuplicatedCommonFiles(outputDirectory, commonDefinitionsPerSystem);
        avroCompiler.deleteBaseEventFiles();
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

        if (!overallResult.isValid()) {
            throw new MojoExecutionException("At least one avro schema is not a valid message. Check log for details");
        }
    }

    private String newTypes(Set<NewMessageTypeVersionDto> newMessageTypeVersionDtos) {
        return newMessageTypeVersionDtos.stream()
                .map(Record::toString)
                .collect(joining("\n", "\n", ""));
    }

    /**
     * Compile all message types of the registry based on the messages descriptors
     *
     * @param avroCompiler the avro compiler used to compile the schemas
     * @throws MojoExecutionException if the process failed
     */
    private void compile(AvroCompiler avroCompiler) throws MojoExecutionException {
        this.commonDefinitionsPerSystem = retrieveCommonDefinitionsGroupedBySystem();
        compileCommonSchemas(avroCompiler, Set.of());
        compileSchemas(avroCompiler, retrieveSchemas(retrieveDescriptors()));
    }

    /**
     * Compile only changed files based on a git diff
     *
     * @param avroCompiler the avro compiler used to compile the schemas
     * @param gitDiff      Git differences relevant for the compiler
     * @throws MojoExecutionException if the process failed
     */
    private void compile(AvroCompiler avroCompiler, GitDiffDto gitDiff) throws MojoExecutionException {
        this.commonDefinitionsPerSystem = retrieveCommonDefinitionsGroupedBySystem();
        compileCommonSchemas(avroCompiler, gitDiff.systems());
        compileSchemas(avroCompiler, retrieveSchemasForNewMessageTypeVersions(gitDiff.newMessageTypeVersions()));
    }

    private List<MessageSchema> retrieveSchemasForNewMessageTypeVersions(Set<NewMessageTypeVersionDto> newMessageTypeVersions) throws MojoExecutionException {
        List<MessageSchema> list = new ArrayList<>();
        for (NewMessageTypeVersionDto dto : newMessageTypeVersions) {
            list.addAll(retrieveSchemasForNewMessageTypeVersion(dto.descriptorPath(), dto.typeDescriptor(), dto.version()));
        }
        return list;
    }

    private List<MessageSchema> retrieveSchemasForNewMessageTypeVersion(Path descriptorPath, TypeDescriptor typeDescriptor, String newVersion) throws MojoExecutionException {
        List<MessageSchema> schemas = new ArrayList<>();
        addValueSchema(descriptorPath, typeDescriptor, newVersion, schemas);
        addKeySchema(descriptorPath, typeDescriptor, newVersion, schemas);
        return schemas;
    }

    private void addValueSchema(Path descriptorPath, TypeDescriptor typeDescriptor, String newVersion, List<MessageSchema> schemas) throws MojoExecutionException {
        Optional<TypeVersion> typeVersionValueSchema = typeDescriptor.getVersions().stream()
                .filter(v -> v.getVersion().equals(newVersion)).findFirst();
        if (typeVersionValueSchema.isPresent()) {
            final TypeVersion version = typeVersionValueSchema.get();
            schemas.add(descriptorToMessageSchema(schemaFile(descriptorPath, version.getValueSchema()), typeDescriptor, version));
        }
    }

    private void addKeySchema(Path descriptorPath, TypeDescriptor typeDescriptor, String newVersion, List<MessageSchema> schemas) throws MojoExecutionException {
        Optional<TypeVersion> typeVersionKeySchema = typeDescriptor.getVersions().stream()
                .filter(v -> v.getKeySchema() != null && v.getVersion().equals(newVersion)).findFirst();
        if (typeVersionKeySchema.isPresent()) {
            final TypeVersion version = typeVersionKeySchema.get();
            if (compileKeySchema(version)) {
                schemas.add(descriptorToMessageSchema(schemaFile(descriptorPath, version.getKeySchema()), typeDescriptor, version));
            }
        }
    }

    private static File schemaFile(Path descriptorPath, String schemaFile) {
        return new File(descriptorPath.getParent().toFile(), schemaFile);
    }

    /**
     * Retrieve all common files ending with avdl for all systems and save the files grouped by system in a map
     *
     * @return a map with all common files grouped by systems
     * @throws MojoExecutionException if the source directory is not available
     */
    private Map<String, List<Path>> retrieveCommonDefinitionsGroupedBySystem() throws MojoExecutionException {
        try (Stream<Path> stream = Files.walk(Paths.get(sourceDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            return stream
                    .filter(path -> path.toString().contains(MessageTypeRegistryConstants.COMMON_DIR_NAME) && FilenameUtils.getExtension(path.getFileName().toString()).equals("avdl"))
                    .collect(Collectors.groupingBy(file -> file.getParent().getParent().getFileName().toString(), Collectors.mapping(file -> file, Collectors.toList())));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot walk through the source directory", e);
        }
    }

    private void compileCommonSchemas(AvroCompiler avroCompiler, Set<String> changedSystemsToCompile) throws MojoExecutionException {
        getLog().info("Compile common schemas for " + commonDefinitionsPerSystem.entrySet().size() + " systems");
        for (Map.Entry<String, List<Path>> entry : commonDefinitionsPerSystem.entrySet()) {

            if (!changedSystemsToCompile.isEmpty() && !changedSystemsToCompile.contains(entry.getKey())) {
                getLog().info("... System " + entry.getKey() + " not changed. Skipping...");
            } else {

                List<MessageSchema> schemas = entry.getValue().stream()
                        .map(commonFile -> commonSchemaToMessageSchema(commonFile.toFile(), entry.getKey(), commonFile.getFileName().toString(), RegistryHelper.retrieveVersionFromCommonDefinition(commonFile.toString())))
                        .toList();

                for (MessageSchema schema : schemas) {
                    getLog().debug("Compile common schema: " + schema.getSchema().getAbsolutePath());
                    final Path outputPath = Paths.get(avroCompiler.getOutputDirectory().getAbsolutePath(), retrieveTypeReference(schema).getDefiningSystem(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
                    compileSchema(outputPath, avroCompiler, schema);
                }

                final Path outputPath = Paths.get(avroCompiler.getOutputDirectory().getAbsolutePath(), entry.getKey(), MessageTypeRegistryConstants.COMMON_DIR_NAME);
                PomFileGenerator.generatePomFile(outputPath, getGroupIdPrefixWithTrailingDot() + entry.getKey().toLowerCase(Locale.ROOT), entry.getKey() + "-messaging-common", "", getCommonLibVersionAsProjectVersion(), this.jeapMessagingVersion);

                getLog().info("+++ Compiled " + entry.getValue().size() + " common schemas for system " + entry.getKey());
            }
        }
    }

    private String getGroupIdPrefixWithTrailingDot() {
        return groupIdPrefix.endsWith(".") ? groupIdPrefix : groupIdPrefix + ".";
    }

    private TypeReference retrieveTypeReference(MessageSchema schema) throws MojoExecutionException {
        final Optional<TypeReference> typeReference = schema.getTypeReference();
        if (typeReference.isPresent()) {
            return typeReference.get();
        } else {
            throw new MojoExecutionException("TypeReference is empty but must be present");
        }
    }

    private Map<String, File> retrieveCommonFilesForSystem(String systemName) throws MojoExecutionException {
        final Path systemCommonDirectoryPath = Paths.get(sourceDirectory.getAbsolutePath(), systemName.toLowerCase(), MessageTypeRegistryConstants.COMMON_DIR_NAME);

        if (!Files.exists(systemCommonDirectoryPath)) {
            return Collections.emptyMap();
        }

        try (Stream<Path> stream = Files.walk(systemCommonDirectoryPath, Integer.MAX_VALUE)) {
            return stream
                    .filter(file -> FilenameUtils.getExtension(file.getFileName().toString()).equals("avdl"))
                    .collect(toMap(filepath -> filepath.getFileName().toString(), Path::toFile));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot get the common files: " + e.getMessage(), e);
        }
    }

    private List<Path> retrieveDescriptors() throws MojoExecutionException {
        try (Stream<Path> stream = Files.walk(Paths.get(sourceDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            return stream
                    .filter(f -> FilenameUtils.getExtension(f.getFileName().toString()).equals("json"))
                    .toList();
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot walk through the sourceDirectory: " + e.getMessage(), e);
        }
    }

    private MessageSchema commonSchemaToMessageSchema(File schemaFile, String system, String messageName, String version) {
        return MessageSchema.builder()
                .schema(schemaFile)
                .typeReference(new TypeReference(system, messageName, version))
                .importPath(Map.of())
                .build();
    }

    private MessageSchema descriptorToMessageSchema(File schemaFile, TypeDescriptor typeDescriptor, TypeVersion version) throws MojoExecutionException {
        MessageTypeMetadata messageTypeMetadata = MessageTypeMetadataProvider
                .createMessageTypeMetadata(typeDescriptor, version, currentBranch, commitId, gitUrl);
        return MessageSchema.builder()
                .schema(schemaFile)
                .messageTypeMetadata(messageTypeMetadata)
                .typeReference(new TypeReference(typeDescriptor.getDefiningSystem(), typeDescriptor.getName(), version.getVersion()))
                .importPath(retrieveCommonFilesForSystem(typeDescriptor.getDefiningSystem()))
                .build();
    }

    private List<MessageSchema> retrieveSchemas(List<Path> descriptors) throws MojoExecutionException {
        List<MessageSchema> schemas = new ArrayList<>();
        for (Path descriptor : descriptors) {
            schemas.addAll(retrieveSchemasForDescriptor(descriptor, TypeDescriptorFactory.getTypeDescriptor(descriptor)));
        }
        return schemas;
    }

    private List<MessageSchema> retrieveSchemasForDescriptor(Path descriptor, TypeDescriptor typeDescriptor) throws MojoExecutionException {
        List<MessageSchema> schemas = new ArrayList<>();
        for (TypeVersion version : typeDescriptor.getVersions()) {
            schemas.add(descriptorToMessageSchema(schemaFile(descriptor, version.getValueSchema()), typeDescriptor, version));

            if (compileKeySchema(version)) {
                schemas.add(descriptorToMessageSchema(schemaFile(descriptor, version.getKeySchema()), typeDescriptor, version));
            }
        }
        return schemas;
    }

    private boolean compileKeySchema(TypeVersion version) {
        return version.getKeySchema() != null && !version.getKeySchema().startsWith("ch.");
    }

    private void compileSchemas(AvroCompiler avroCompiler, List<MessageSchema> schemas) throws MojoExecutionException {
        getLog().info("Compiling " + schemas.size() + " schemas");
        for (MessageSchema schema : schemas) {
            getLog().debug("Compile schema " + schema.getSchema().getAbsolutePath());
            final TypeReference typeReference = retrieveTypeReference(schema);
            final Path outputPath = Paths.get(avroCompiler.getOutputDirectory().getAbsolutePath(), typeReference.getDefiningSystem().toLowerCase(Locale.ROOT), typeReference.getName(), typeReference.getVersion());
            String groupId = getGroupIdPrefixWithTrailingDot() + typeReference.getDefiningSystem().toLowerCase(Locale.ROOT);
            PomFileGenerator.generatePomFile(outputPath, groupId,
                    camelCase2Snake(typeReference.getName()),
                    getDependencyDefinition(typeReference),
                    getArtifactVersion(typeReference.getVersion()),
                    this.jeapMessagingVersion);
            compileSchema(outputPath, avroCompiler, schema);
        }
        getLog().info("Compiled all schemas");
    }

    private String getDependencyDefinition(TypeReference typeReference) {
        String dependency = "";
        if (commonLibAvailableForSystem(typeReference.getDefiningSystem())) {
            String groupId = getGroupIdPrefixWithTrailingDot() + typeReference.getDefiningSystem().toLowerCase(Locale.ROOT);
            dependency = PomFileGenerator.getCommonDependency(
                    groupId,
                    typeReference.getDefiningSystem().toLowerCase(Locale.ROOT),
                    getCommonLibVersionAsDependencyVersion());
        }

        return dependency;
    }

    private void compileSchema(Path outputPath, AvroCompiler avroCompiler, MessageSchema messageSchema) throws MojoExecutionException {
        AvroCompiler.AvroCompilerBuilder avroCompilerBuilder = avroCompiler.toBuilder()
                .outputDirectory(Paths.get(outputPath.toString(), "src", "main", "java").toFile());
        if (messageSchema.getMessageTypeMetadata().isPresent()) {
            avroCompilerBuilder.additionalTool(messageSchema.getMessageTypeMetadata().get());
        }

        avroCompiler = avroCompilerBuilder.build();

        try (ImportClassLoader importClassLoader = getImportClassLoader(messageSchema)) {
            compileIdl(avroCompiler, messageSchema.getSchema(), importClassLoader);
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot create classpath loader: " + e.getMessage(), e);
        }
    }

    private String getArtifactVersion(String typeReferenceVersion) {
        if (isBuildOnTrunk()) {
            return typeReferenceVersion;
        } else {
            return String.format("%s-%s-SNAPSHOT", typeReferenceVersion, getSanitizedCurrentBranchName());
        }
    }

    private String getCommonLibVersionAsDependencyVersion() {
        if (isBuildOnTrunk()) {
            return "[0,)";
        } else {
            return getCommonLibSnapshotVersion();
        }
    }

    private String getCommonLibVersionAsProjectVersion() {
        if (isBuildOnTrunk()) {
            return commonLibVersion;
        } else {
            return getCommonLibSnapshotVersion();
        }
    }

    private boolean isBuildOnTrunk() {
        return trunkBranchName.equals(currentBranch);
    }

    private String getCommonLibSnapshotVersion() {
        return String.format("%s-%s-SNAPSHOT", commonLibVersion, getSanitizedCurrentBranchName());
    }

    private String getSanitizedCurrentBranchName() {
        return currentBranch.replaceAll("[^A-Za-z0-9-_]", "_");
    }

    private boolean commonLibAvailableForSystem(String definingSystem) {
        return commonDefinitionsPerSystem.containsKey(definingSystem.toLowerCase(Locale.ROOT));
    }

    private ImportClassLoader getImportClassLoader(MessageSchema messageSchema) throws IOException, DependencyResolutionRequiredException {
        ImportClassLoader importClassLoader = new ImportClassLoader(sourceDirectory, project.getRuntimeClasspathElements());
        for (String filename : messageSchema.getImportPath().keySet()) {
            importClassLoader.addImportFile(filename, messageSchema.getImportPath().get(filename));
        }
        return importClassLoader;
    }

    private void compileIdl(AvroCompiler avroCompiler, File file, ImportClassLoader importClassLoader) throws MojoExecutionException {
        try {
            IdlFileParser idlFileParser = new IdlFileParser(importClassLoader);
            Protocol protocol = idlFileParser.parseIdlFile(file);
            if (isValid(RecordCollection.of(protocol), file)) {
                //We cannot skip compile when the file has not changed
                //as there might include which have changed
                getLog().debug("Compile protocol " + protocol.getName() + " from IDL file " + file.getAbsolutePath());
                protocol.getTypes().forEach(t -> getLog().debug("Type " + t.getName() + " is in this record"));
                avroCompiler.compileProtocol(protocol, null);
            }
        } catch (IOException | ParseException e) {
            throw compileException(e, file);
        }
    }

    private MojoExecutionException compileException(Exception e, File file) {
        return new MojoExecutionException("Could not compile file " + file.getAbsolutePath(), e);
    }

    private boolean isValid(RecordCollection recordCollection, File src) {
        final ValidationResult result = SchemaValidator.validate(recordCollection);
        if (!result.isValid()) {
            result.getErrors().stream()
                    .map(e -> "In file " + src.getAbsolutePath() + ": " + e)
                    .forEach(getLog()::error);
        }
        overallResult = ValidationResult.merge(result, overallResult);
        return result.isValid();
    }

    private String camelCase2Snake(String input) {
        if (isBlank(input)) {
            return input;
        }
        return input.replaceAll("([A-Z])", "-$1").replaceFirst("^-", "").toLowerCase();
    }
}
