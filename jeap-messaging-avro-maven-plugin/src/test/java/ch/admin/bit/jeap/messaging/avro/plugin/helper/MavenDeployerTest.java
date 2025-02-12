package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MavenDeployerTest {

    private static final String MAVEN_EXECUTABLE = "../mvnw";
    private static final String GOAL = "deploy";
    // Note: This would be a settings.xml file. As the file must exist, pom.xml is used for the test.
    private static final String SETTINGS_FILE = "pom.xml";
    private static final String HTTP_FAKE_PROXY_PROPERTY = "http.fake.proxy.property";
    private static final String HTTP_FAKE_NON_PROXY_PROPERTY = "http.fake.nonProxy.property";

    @Mock
    private Invoker invoker;
    @Mock
    private InvocationResult invocationResult;
    @Captor
    private ArgumentCaptor<InvocationRequest> invocationRequestArgument;

    private MavenDeployer mavenDeployer;

    @BeforeEach
    void setUp() throws MavenInvocationException {
        mavenDeployer = new MavenDeployer(new SystemStreamLog(), GOAL, true, MAVEN_EXECUTABLE, SETTINGS_FILE, "my-profile") {
            @Override
            Invoker createInvoker() {
                return invoker;
            }
        };
    }

    @BeforeAll
    static void prepareFakeProxyProperties() throws MojoExecutionException {
        System.setProperty(HTTP_FAKE_PROXY_PROPERTY, "foo-proxy");
        System.setProperty(HTTP_FAKE_NON_PROXY_PROPERTY, "non-proxy");
    }

    @AfterAll
    static void resetFakeProxyProperties() {
        System.clearProperty(HTTP_FAKE_PROXY_PROPERTY);
        System.clearProperty(HTTP_FAKE_NON_PROXY_PROPERTY);
    }

    @Test
    void deployProjects() throws Exception {
        List<Path> twoPoms = List.of(Paths.get("proj-1"), Paths.get("proj-2"));
        when(invoker.execute(invocationRequestArgument.capture()))
                .thenReturn(invocationResult);

        List<InvocationResult> results = mavenDeployer.deployProjects(twoPoms);

        assertEquals(2, results.size());
        assertEquals(2, invocationRequestArgument.getAllValues().size());
        InvocationRequest invocationRequest = invocationRequestArgument.getValue();
        assertEquals(new File(MAVEN_EXECUTABLE), invocationRequest.getMavenExecutable());
        assertEquals(new File(SETTINGS_FILE), invocationRequest.getGlobalSettingsFile());
        assertEquals(List.of("my-profile"), invocationRequest.getProfiles());
        assertEquals(List.of(GOAL), invocationRequest.getGoals());
        assertEquals("foo-proxy", invocationRequest.getProperties().get(HTTP_FAKE_PROXY_PROPERTY));
        assertEquals("non-proxy", invocationRequest.getProperties().get(HTTP_FAKE_NON_PROXY_PROPERTY));
        assertEquals("true", invocationRequest.getProperties().get("maven.test.skip"));
    }

    @Test
    void deployProjects_whenExceptionIsThrownForExecution_thenShouldPropagateException() throws Exception {
        List<Path> twoPoms = List.of(Paths.get("proj-1"), Paths.get("proj-2"));
        when(invoker.execute(invocationRequestArgument.capture()))
                .thenThrow(new MavenInvocationException("test"));

        MojoExecutionException mojoExecutionException = assertThrows(MojoExecutionException.class,
                () -> mavenDeployer.deployProjects(twoPoms));
        assertEquals("Error during Maven Invocation: test", mojoExecutionException.getMessage());
    }

    @Test
    void deployProjects_whenReturnCodeIsNonZeroForExecution_thenShouldThrowException() throws Exception {
        List<Path> twoPoms = List.of(Paths.get("proj-1"), Paths.get("proj-2"));
        when(invoker.execute(invocationRequestArgument.capture()))
                .thenReturn(invocationResult);
        when(invocationResult.getExitCode()).thenReturn(2);

        MojoExecutionException mojoExecutionException = assertThrows(MojoExecutionException.class,
                () -> mavenDeployer.deployProjects(twoPoms));
        assertEquals("Build failed with exitCode 2", mojoExecutionException.getMessage());
    }
}
