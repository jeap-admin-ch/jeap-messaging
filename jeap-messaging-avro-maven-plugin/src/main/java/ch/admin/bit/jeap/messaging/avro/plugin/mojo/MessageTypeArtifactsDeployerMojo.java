package ch.admin.bit.jeap.messaging.avro.plugin.mojo;

import ch.admin.bit.jeap.messaging.avro.plugin.helper.MavenDeployer;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
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
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Mojo(name = "deploy-message-type-artifacts", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class MessageTypeArtifactsDeployerMojo extends AbstractMojo {

    public static final String POM_XML_FILE_NAME = "pom.xml";

    @Parameter(name = "sourcesDirectory", defaultValue = "${project.build.directory}/generated-sources")
    @SuppressWarnings("unused")
    private File sourcesDirectory;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    @SuppressWarnings("unused")
    private MavenProject project;

    @Parameter(name = "mavenDeployGoal", defaultValue = "install")
    @SuppressWarnings("unused")
    private String mavenDeployGoal;

    @Parameter(name = "mavenExecutable")
    @SuppressWarnings("unused")
    private String mavenExecutable;

    @Parameter(name = "mavenGlobalSettingsFile")
    @SuppressWarnings("unused")
    private String mavenGlobalSettingsFile;

    /**
     * Whether to deploy message type artifacts to the repository with parallel threads or not. Mostly useful when
     * deploying a full message type repository with a lot of message types initially.
     */
    @Parameter(name = "parallel", defaultValue = "false")
    @SuppressWarnings("unused")
    private boolean parallel;

    @Override
    public void execute() throws MojoExecutionException {
        if (!sourcesDirectory.exists()) {
            return;
        }
        MavenDeployer deployer = new MavenDeployer(getLog(), mavenDeployGoal, parallel, mavenExecutable, mavenGlobalSettingsFile);
        deployCommonLibraries(deployer);
        deployLibraries(deployer);
    }

    private void deployCommonLibraries(MavenDeployer deployer) throws MojoExecutionException {
        try (Stream<Path> walk = Files.walk(Paths.get(sourcesDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            List<Path> poms = walk.filter(path -> isCommonLibrary(path) && path.toString().endsWith(POM_XML_FILE_NAME)).collect(toList());
            getLog().info("Deploying " + poms.size() + " common maven projects.");
            deployer.deployProjects(poms);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot parse the output directory for the pom.xml: " + e.getMessage(), e);
        }

    }

    private void deployLibraries(MavenDeployer deployer) throws MojoExecutionException {
        try (Stream<Path> walk = Files.walk(Paths.get(sourcesDirectory.getAbsolutePath()), Integer.MAX_VALUE)) {
            List<Path> poms = walk.filter(path -> !isCommonLibrary(path) && path.toString().endsWith(POM_XML_FILE_NAME)).collect(toList());
            getLog().info("Deploying " + poms.size() + " maven projects.");
            deployer.deployProjects(poms);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot parse the output directory for the pom.xml: " + e.getMessage(), e);
        }

    }

    private boolean isCommonLibrary(Path path) {
        return path.toString().contains(MessageTypeRegistryConstants.COMMON_DIR_NAME);
    }
}
