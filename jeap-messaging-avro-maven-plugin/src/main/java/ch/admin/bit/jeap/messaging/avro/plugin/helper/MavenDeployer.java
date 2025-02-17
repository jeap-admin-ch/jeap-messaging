package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MavenDeployer {

    private final Log log;
    private final String mavenDeployGoal;
    private final boolean parallel;
    private final String mavenExecutable;
    private final String mavenGlobalSettingsFile;
    private final String profile;

    public MavenDeployer(Log log, String mavenDeployGoal, boolean parallel, String mavenExecutable, String mavenGlobalSettingsFile, String profile) {
        this.log = log;
        this.mavenDeployGoal = mavenDeployGoal;
        this.parallel = parallel;
        this.mavenExecutable = mavenExecutable;
        this.mavenGlobalSettingsFile = mavenGlobalSettingsFile;
        this.profile = profile;
    }

    public List<InvocationResult> deployProjects(List<Path> poms) {
        if (poms.isEmpty()) {
            return List.of();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(parallel ? 1 : 10);

        // Run one build to cache the dependencies in the local repository
        // The local repository does not support concurrency (all message types have the same set of dependencies)
        Path path = poms.getFirst();
        poms = poms.subList(1, poms.size());
        InvocationRequest invocationRequest = toInvocationRequest(path);
        List<InvocationResult> results = new ArrayList<>();
        InvocationResult firstResult = awaitAndGetResult(runMavenInvoker(executorService, invocationRequest));
        results.add(firstResult);

        // Run the rest of the builds in parallel to speed things up
        poms.stream()
                .map(this::toInvocationRequest)
                .map(r -> runMavenInvoker(executorService, r))
                .map(MavenDeployer::awaitAndGetResult)
                .forEach(results::add);

        log.info("Executed " + results.size() + " tasks");
        return results;
    }

    @SneakyThrows
    private static InvocationResult awaitAndGetResult(Future<InvocationResult> r) {
        try {
            return r.get();
        } catch (ExecutionException ex) {
            // If any of the builds of a message type failed, throw the causing MojoExecutionException
            throw ex.getCause();
        }
    }

    private Future<InvocationResult> runMavenInvoker(ExecutorService executorService, InvocationRequest request) {
        log.info("Executing maven request for pom %s: mnv=%s args=%s profiles=%s goals=%s props=%s".formatted(
                request.getPomFile(), getMavenExecutable(), request.getArgs(),
                request.getProfiles() == null ? "" : request.getProfiles(), request.getGoals(), request.getProperties()));
        return executorService.submit(() -> executeRequest(request));
    }

    @SneakyThrows
    private InvocationResult executeRequest(InvocationRequest request) {
        Invoker invoker = createInvoker();
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Build failed with exitCode " + result.getExitCode());
            }
            return result;
        } catch (MavenInvocationException e) {
            throw new MojoExecutionException("Error during Maven Invocation: " + e.getMessage(), e);
        }
    }

    Invoker createInvoker() {
        return new DefaultInvoker();
    }

    private InvocationRequest toInvocationRequest(Path pomPath) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setPomFile(pomPath.toFile());
        request.setMavenExecutable(getMavenExecutable());
        request.setGlobalSettingsFile(getMavenGlobalSettingsFile());
        if (profile != null) {
            request.setProfiles(List.of(profile));
        }
        request.setGoals(List.of(mavenDeployGoal));
        Properties properties = new Properties();
        // No tests in generated message types, skip
        properties.setProperty("maven.test.skip", "true");
        // Colored output
        properties.setProperty("style.color", "always");
        properties.setProperty("jansi.force", "true");
        // Pass proxy properties to invoked maven instance
        Map<String, String> proxyProperties = System.getProperties().entrySet().stream()
                .filter(e -> e.getKey().toString().matches("^http.*[pP]roxy.*$")) // NOSONAR
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        properties.putAll(proxyProperties);
        request.setProperties(properties);
        return request;
    }

    private File getMavenExecutable() {
        if (!StringUtils.isBlank(this.mavenExecutable) && Files.exists(Path.of(this.mavenExecutable))) {
            return Path.of(this.mavenExecutable).toFile();
        }
        return Paths.get(Paths.get("").toAbsolutePath().toString(), "mvnw").toFile();
    }

    private File getMavenGlobalSettingsFile() {
        if (!StringUtils.isBlank(this.mavenGlobalSettingsFile) && Files.exists(Path.of(this.mavenGlobalSettingsFile))) {
            return Path.of(this.mavenGlobalSettingsFile).toFile();
        }
        return Paths.get(Paths.get("").toAbsolutePath().toString(), "settings.xml").toFile();
    }
}
