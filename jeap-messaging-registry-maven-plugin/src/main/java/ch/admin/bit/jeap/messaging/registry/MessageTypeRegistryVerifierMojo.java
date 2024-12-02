package ch.admin.bit.jeap.messaging.registry;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.verifier.DescriptorDirectoryValidator;
import ch.admin.bit.jeap.messaging.registry.verifier.ValidationContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Mojo(name = "registry", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class MessageTypeRegistryVerifierMojo extends AbstractMojo {
    @SuppressWarnings("unused")
    @Getter(AccessLevel.PROTECTED)
    @Parameter(name = "descriptorDirectory", defaultValue = "${basedir}/descriptor")
    private File descriptorDirectory;
    @SuppressWarnings("unused")
    @Parameter(name = "gitUrl")
    @Setter(AccessLevel.PACKAGE)
    private String gitUrl;
    @SuppressWarnings("unused")
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        try (ImportClassLoader importClassLoader = new ImportClassLoader(getDescriptorDirectory(),
                getProject().getRuntimeClasspathElements())) {
            ValidationContext validationContext = ValidationContext.builder()
                    .descriptorDir(descriptorDirectory)
                    .oldDescriptorDir(getFolderToCompareTo())
                    .importClassLoader(importClassLoader)
                    .log(getLog())
                    .build();
            ValidationResult overallResult = DescriptorDirectoryValidator.validate(validationContext);
            if (!overallResult.isValid()) {
                List<String> errors = overallResult.getErrors();
                errors.forEach(getLog()::error);
                String errorSummary = String.join("\n", errors);
                int errorCount = errors.size();
                throw new MojoExecutionException("The message type registry is not in a clean state (" + errorCount + " errors). Check log for details.\n" + errorSummary);
            }
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot get runtime classpath", e);
        }
    }

    private File getFolderToCompareTo() throws MojoExecutionException {
        if (gitUrl == null) {
            getLog().warn("No GIT repo set, cannot compare to old state");
            return descriptorDirectory;
        }
        try {
            File tempDir = Files.createTempDirectory("master").toFile();
            FileUtils.forceDeleteOnExit(tempDir);
            Git.cloneRepository()
                    .setURI(gitUrl)
                    .setBranch("master")
                    .setDirectory(tempDir)
                    .call();
            return new File(tempDir, descriptorDirectory.getName());
        } catch (IOException | GitAPIException e) {
            throw new MojoExecutionException("Cannot checkout old repo", e);
        }
    }
}
