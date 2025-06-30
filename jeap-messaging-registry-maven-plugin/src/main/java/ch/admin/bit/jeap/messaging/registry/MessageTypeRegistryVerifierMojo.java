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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Mojo(name = "registry", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class MessageTypeRegistryVerifierMojo extends AbstractMojo {
    private final Log log = new SystemStreamLog();
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
    @SuppressWarnings("unused")
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "master", required = true, readonly = true)
    private String trunkBranchName;
    @Getter(AccessLevel.PROTECTED)
    @Parameter(defaultValue = "true")
    @SuppressWarnings("unused")
    private boolean failOnUnusedImports;
    @Parameter(defaultValue = "MESSAGE_TYPE_REPO_GIT_TOKEN")
    @SuppressWarnings("unused")
    private String messageTypeRepoGitTokenEnvVariableName;

    @Override
    public void execute() throws MojoExecutionException {
        try (ImportClassLoader importClassLoader = new ImportClassLoader(getDescriptorDirectory(),
                getProject().getRuntimeClasspathElements())) {
            ValidationContext validationContext = ValidationContext.builder()
                    .descriptorDir(descriptorDirectory)
                    .oldDescriptorDir(getFolderToCompareTo())
                    .importClassLoader(importClassLoader)
                    .failOnUnusedImports(failOnUnusedImports)
                    .log(getLog())
                    .trunkBranchName(trunkBranchName)
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
            getLog().warn("No Git repo set, cannot compare to previous state");
            return descriptorDirectory;
        }
        log.info("Comparing message type registry to Git repository '%s'.".formatted(gitUrl));
        try {
            File tempDir = Files.createTempDirectory(trunkBranchName).toFile();
            FileUtils.forceDeleteOnExit(tempDir);
            String token = getGitToken();
            if (token != null) {
                cloneMessageTypeRepoWithToken(tempDir, token);
            } else {
                cloneMessageTypeRepoWithSystemGit(tempDir);
            }
            log.info("Cloned message type repository branch '" + trunkBranchName + "' successfully.");
            return new File(tempDir, descriptorDirectory.getName());
        } catch (IOException | GitAPIException e) {
            throw new MojoExecutionException("Cannot checkout old repo", e);
        }
    }

    private String getGitToken() {
        log.info("The message type repo git token env variable name configured is: " + messageTypeRepoGitTokenEnvVariableName);
        String token = Optional.ofNullable(System.getenv(messageTypeRepoGitTokenEnvVariableName))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        log.info("The env variable " + messageTypeRepoGitTokenEnvVariableName + " is " +
                (token != null ? "set." : "not set."));
        return token;
    }

    private void cloneMessageTypeRepoWithToken(File targetDir, String token) throws GitAPIException {
        log.info("Using JGit with a token to clone the message type repository.");
        //noinspection EmptyTryBlock
        try (Git ignored = Git.cloneRepository()
                .setURI(gitUrl)
                .setBranch(trunkBranchName)
                .setDirectory(targetDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("no-username-when-using-token", token))
                .call()) {
            // No action needed, just to automatically close the resource
        }
    }

    private void cloneMessageTypeRepoWithSystemGit(File targetDir) throws IOException {
        log.info("Using a system Git process to clone the message type repository.");
        ProcessBuilder pb = new ProcessBuilder("git", "clone", "--branch", trunkBranchName, "--single-branch",
                gitUrl, targetDir.getAbsolutePath());
        Process process = pb.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String message = "The Git clone process failed to clone the repository. Exit code: " + exitCode;
                log.error(message);
                throw new IllegalStateException(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while waiting for the Git clone process to finish.";
            log.error(message);
            throw new IllegalStateException(message, e);
        }
    }

}
