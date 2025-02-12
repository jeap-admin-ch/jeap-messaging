package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import org.apache.commons.text.StringSubstitutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class PomFileGenerator {

    public static final String POM_XML_FILE_NAME = "pom.xml";

    private final Path outputPath;
    private final File pomTemplateFile;
    private final Log log;

    public PomFileGenerator(Path outputPath, File pomTemplateFile, Log log) {
        this.outputPath = outputPath;
        this.pomTemplateFile = pomTemplateFile;
        this.log = log;
    }

    public void generatePomFile(String groupId, String artefactId,
                                String dependency, String version, String jeapMessagingVersion) throws MojoExecutionException {
        try {
            String pomContent = getPomXmlContent(groupId, artefactId, dependency, version, jeapMessagingVersion);
            Path path = Paths.get(outputPath.toString(), POM_XML_FILE_NAME);
            Files.createDirectories(outputPath);
            Files.write(path, pomContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot write pom.xml to directory: " + e.getMessage(), e);
        }
    }

    private String getPomXmlContent(String groupId, String artifactId, String dependency, String version, String jeapMessagingVersion) throws IOException {
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

    private String loadPomTemplate() throws IOException {
        try (InputStream resourceAsStream = openPomTemplateStream()) {
            byte[] content = resourceAsStream.readAllBytes();
            return new String(content);
        }
    }

    private InputStream openPomTemplateStream() throws IOException {
        if (pomTemplateFile != null) {
            if (!pomTemplateFile.isFile()) {
                throw new IllegalArgumentException("Messagetype pom.xml template file does not exist at %s"
                        .formatted(pomTemplateFile.getAbsolutePath()));
            }
            log.info("Using pom template at " + pomTemplateFile + " to generate " + outputPath);
            return new FileInputStream(pomTemplateFile);
        }
        log.info("Using built-in pom template file to generate " + outputPath);
        return PomFileGenerator.class.getClassLoader()
                .getResourceAsStream("pom.template.messagetype.xml");
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
        return StringSubstitutor.replace(template, params);
    }
}
