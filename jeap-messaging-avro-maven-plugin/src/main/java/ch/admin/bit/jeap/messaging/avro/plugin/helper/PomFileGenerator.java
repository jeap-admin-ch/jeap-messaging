package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import lombok.experimental.UtilityClass;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        return String.format(pomTemplate, groupId, artifactId, dependency, version, jeapMessagingVersion);
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
                                      <groupId>%1$s</groupId>
                                      <artifactId>%2$s-messaging-common</artifactId>
                                      <version>%3$s</version>
                                  </dependency>""";

        return String.format(template, groupId, artifactId, version);
    }

}
