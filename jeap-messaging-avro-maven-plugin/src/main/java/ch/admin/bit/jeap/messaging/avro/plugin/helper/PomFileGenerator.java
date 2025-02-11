package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import lombok.experimental.UtilityClass;
import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class PomFileGenerator {

    public static final String POM_XML_FILE_NAME = "pom.xml";

    public static void generatePomFile(Path outputPath, String groupId, String artefactId, String dependency, String version, String jeapMessagingVersion) throws MojoExecutionException {
        try {
            String pomContent = getPomXmlContent(groupId, artefactId, dependency, version, jeapMessagingVersion);
            Path path = Paths.get(outputPath.toString(), POM_XML_FILE_NAME);
            Files.createDirectories(outputPath);
            Files.write(path, pomContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write pom.xml to directory: " + e.getMessage(), e);
        }

    }

    private static String getPomXmlContent(String groupId, String artifactId, String dependency, String version, String jeapMessagingVersion) throws IOException {
        String pomTemplate = loadPomTemplate();
        // Allow comment-quoting the dependency element to make sure the template is valid xml
        pomTemplate = pomTemplate.replace("<!-- ${dependency} -->", "${dependency}");

        Map<String, String> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("artifactId", artifactId);
        params.put("dependency", dependency);
        params.put("version", version);
        params.put("jeapMessagingVersion", jeapMessagingVersion);
        return StringSubstitutor.replace(pomTemplate, params);
    }

    private static String loadPomTemplate() throws IOException {
        try (InputStream resourceAsStream = PomFileGenerator.class.getClassLoader()
                .getResourceAsStream("pom.template.messagetype.xml")) {
            byte[] content = resourceAsStream.readAllBytes();
            return new String(content);
        }
    }

    public static String getCommonDependency(String groupId, String artifactId, String version) {
        String template = """
                                  <dependency>
                                      <groupId>${groupId}</groupId>
                                      <artifactId>${artifactId}-messaging-common</artifactId>
                                      <version>${version}</version>
                                  </dependency>""";

        Map<String, String> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("artifactId", artifactId);
        params.put("version", version);
        return  StringSubstitutor.replace(template, params);
    }
}
