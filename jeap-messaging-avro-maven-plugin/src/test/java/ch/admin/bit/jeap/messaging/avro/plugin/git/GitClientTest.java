package ch.admin.bit.jeap.messaging.avro.plugin.git;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.lib.Ref;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitClientTest {

    @Test
    void findMostRecentTag_semantic() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                createMockRef("1.0.0"),
                createMockRef("1.2.3"),
                createMockRef("1.15.3"),
                createMockRef("1.1.1")),
                "1.15.3");
    }

    @Test
    void findMostRecentTag_buildTimestamp() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                createMockRef("1.0.0-20240208133822"),
                createMockRef("1.0.0-20240208133823"),
                createMockRef("1.0.0-20240208133826"),
                createMockRef("1.0.0-20240108133826")),
                "1.0.0-20240208133826");
    }

    @Test
    void findMostRecentTag_buildTimestampNewVersion() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.0.0-20240208133822"),
                        createMockRef("1.0.0-20240208133823"),
                        createMockRef("1.0.0-20240208133826"),
                        createMockRef("1.1.0-20240208133826"),
                        createMockRef("1.0.0-20240108133826")),
                "1.1.0-20240208133826");
    }

    @Test
    void findMostRecentTag_buildNumber() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.0.1-53"),
                        createMockRef("1.2.0-67"),
                        createMockRef("1.2.0-68"),
                        createMockRef("1.1.9-99")),
                "1.2.0-68");
    }

    @Test
    void findMostRecentTag_buildMultiple() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("1.30.1-53"),
                        createMockRef("1.29.0-67"),
                        createMockRef("1.4.0-68"),
                        createMockRef("1.9.9-99")),
                "1.30.1-53");
    }

    @Test
    void findMostRecentTag_snapshot() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("0.0.1-657"),
                        createMockRef("0.0.1-SNAPSHOT"),
                        createMockRef("0.0.2-SNAPSHOT"),
                        createMockRef("0.0.1-68"),
                        createMockRef("1.0.0-SNAPSHOT")),
                "0.0.1-657");
    }

    @Test
    void findMostRecentTag_ignoreUnknownTags() throws MojoExecutionException {
        doFindMostRecentTag(List.of(
                        createMockRef("2.0.0-20240208133822"),
                        createMockRef("foo-bar"),
                        createMockRef("foo.bar"),
                        createMockRef("2.dummy.6"),
                        createMockRef("1.98788123"),
                        createMockRef("0.0.443343"),
                        createMockRef("dummy"),
                        createMockRef("1.0-alpha")),
                "2.0.0-20240208133822");
    }

    void doFindMostRecentTag(List<Ref> tags, String expectedResult) throws MojoExecutionException {
        Ref mostRecentTag = GitClient.findMostRecentTag(tags);
        assertThat(mostRecentTag.getName()).isEqualTo(expectedResult);
    }

    private Ref createMockRef(String name) {
        Ref ref = mock(Ref.class);
        when(ref.getName()).thenReturn(name);
        return ref;
    }


}
