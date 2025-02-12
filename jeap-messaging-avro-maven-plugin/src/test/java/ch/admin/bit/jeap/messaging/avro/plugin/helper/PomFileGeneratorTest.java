package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PomFileGeneratorTest {

    @Test
    void generatePomFile_withoutDependency() throws MojoExecutionException, IOException {
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>myGroupId</groupId>
                    <artifactId>myArtifactId</artifactId>
                    <version>myVersion</version>
                    <packaging>jar</packaging>
                
                    <licenses>
                        <license>
                            <name>Apache License, Version 2.0</name>
                            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
                        </license>
                    </licenses>
                
                    <properties>
                        <java.version>21</java.version>
                        <maven-compiler-plugin.version>3.8.1</maven-compiler-plugin.version>
                        <maven-source-plugin.version>3.2.1</maven-source-plugin.version>
                        <maven-jar-plugin.version>3.4.2</maven-jar-plugin.version>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>ch.admin.bit.jeap</groupId>
                            <artifactId>jeap-messaging-infrastructure-kafka</artifactId>
                            <version>jeapMessagingVersion</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>${maven-compiler-plugin.version}</version>
                                <configuration>
                                    <source>${java.version}</source>
                                    <target>${java.version}</target>
                                    <testSource>${java.version}</testSource>
                                    <testTarget>${java.version}</testTarget>
                                </configuration>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-jar-plugin</artifactId>
                                <version>${maven-jar-plugin.version}</version>
                                <configuration>
                                    <archive>
                                        <manifest>
                                            <addDefaultEntries>true</addDefaultEntries>
                                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                                            <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                                        </manifest>
                                        <manifestEntries>
                                            <Bundle-License>https://www.apache.org/licenses/LICENSE-2.0</Bundle-License>
                                        </manifestEntries>
                                    </archive>
                                </configuration>
                                <executions>
                                    <execution>
                                        <id>additional-artifact-with-classifier</id>
                                        <goals>
                                            <goal>jar</goal>
                                        </goals>
                                        <configuration>
                                            <classifier>myVersion</classifier>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                            <plugin>
                                <artifactId>maven-source-plugin</artifactId>
                                <version>${maven-source-plugin.version}</version>
                                <executions>
                                    <execution>
                                        <id>attach-sources</id>
                                        <goals>
                                            <goal>jar-no-fork</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                    <distributionManagement>
                        <repository>
                            <id>${releaseRepositoryId}</id>
                            <name>${releaseRepositoryName}</name>
                            <url>${releaseRepositoryUrl}</url>
                        </repository>
                        <snapshotRepository>
                            <id>${snapshotRepositoryId}</id>
                            <name>${snapshotRepositoryName}</name>
                            <!--suppress UnresolvedMavenProperty -->
                            <url>${snapshotRepositoryUrl}</url>
                        </snapshotRepository>
                    </distributionManagement>
                </project>""";

        Path outputPath = Path.of("", "target", "unit-test-pom");
        PomFileGenerator pomFileGenerator = new PomFileGenerator(outputPath, null, new SystemStreamLog());
        pomFileGenerator.generatePomFile("myGroupId", "myArtifactId", "", "myVersion", "jeapMessagingVersion");
        Path filename = Path.of(outputPath.toString(), "pom.xml");
        assertThat(Files.exists(filename))
                .isTrue();
        assertThat(Files.readString(filename))
                .isEqualToIgnoringWhitespace(expected);
    }

    @Test
    void generatePomFile_withDependency() throws MojoExecutionException, IOException {
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>myGroupId</groupId>
                    <artifactId>myArtifactId</artifactId>
                    <version>myVersion</version>
                    <packaging>jar</packaging>
                
                    <licenses>
                        <license>
                            <name>Apache License, Version 2.0</name>
                            <url>https://www.apache.org/licenses/LICENSE-2.0</url>
                        </license>
                    </licenses>
                
                    <properties>
                        <java.version>21</java.version>
                        <maven-compiler-plugin.version>3.8.1</maven-compiler-plugin.version>
                        <maven-source-plugin.version>3.2.1</maven-source-plugin.version>
                        <maven-jar-plugin.version>3.4.2</maven-jar-plugin.version>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>ch.admin.bit.jeap</groupId>
                            <artifactId>jeap-messaging-infrastructure-kafka</artifactId>
                            <version>jeapMessagingVersion</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>myDepGroupId</groupId>
                            <artifactId>myDepArtifactId-messaging-common</artifactId>
                            <version>myDepVersion</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>${maven-compiler-plugin.version}</version>
                                <configuration>
                                    <source>${java.version}</source>
                                    <target>${java.version}</target>
                                    <testSource>${java.version}</testSource>
                                    <testTarget>${java.version}</testTarget>
                                </configuration>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-jar-plugin</artifactId>
                                <version>${maven-jar-plugin.version}</version>
                                <configuration>
                                    <archive>
                                        <manifest>
                                            <addDefaultEntries>true</addDefaultEntries>
                                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                                            <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                                        </manifest>
                                        <manifestEntries>
                                            <Bundle-License>https://www.apache.org/licenses/LICENSE-2.0</Bundle-License>
                                        </manifestEntries>
                                    </archive>
                                </configuration>
                                <executions>
                                    <execution>
                                        <id>additional-artifact-with-classifier</id>
                                        <goals>
                                            <goal>jar</goal>
                                        </goals>
                                        <configuration>
                                            <classifier>myVersion</classifier>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                            <plugin>
                                <artifactId>maven-source-plugin</artifactId>
                                <version>${maven-source-plugin.version}</version>
                                <executions>
                                    <execution>
                                        <id>attach-sources</id>
                                        <goals>
                                            <goal>jar-no-fork</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                    <distributionManagement>
                        <repository>
                            <id>${releaseRepositoryId}</id>
                            <name>${releaseRepositoryName}</name>
                            <url>${releaseRepositoryUrl}</url>
                        </repository>
                        <snapshotRepository>
                            <id>${snapshotRepositoryId}</id>
                            <name>${snapshotRepositoryName}</name>
                            <!--suppress UnresolvedMavenProperty -->
                            <url>${snapshotRepositoryUrl}</url>
                        </snapshotRepository>
                    </distributionManagement>
                </project>""";

        String dependency = PomFileGenerator.getCommonDependency("myDepGroupId", "myDepArtifactId", "myDepVersion");
        Path outputPath = Path.of("", "target", "unit-test-pom");
        PomFileGenerator pomFileGenerator = new PomFileGenerator(outputPath, null, new SystemStreamLog());
        pomFileGenerator.generatePomFile("myGroupId", "myArtifactId", dependency, "myVersion", "jeapMessagingVersion");
        Path filename = Path.of(outputPath.toString(), "pom.xml");
        assertThat(Files.exists(filename))
                .isTrue();
        assertThat(Files.readString(filename))
                .isEqualToIgnoringWhitespace(expected);
    }

    @Test
    void generatePomFile_fromCustomTemplateFile_withDependency() throws MojoExecutionException, IOException {
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                
                    <groupId>myGroupId</groupId>
                    <artifactId>myArtifactId</artifactId>
                    <version>myVersion</version>
                    <packaging>jar</packaging>
                
                    <dependencies>
                        <dependency>
                            <groupId>ch.admin.bit.jeap</groupId>
                            <artifactId>jeap-messaging-infrastructure-kafka</artifactId>
                            <version>jeapMessagingVersion</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>myDepGroupId</groupId>
                            <artifactId>myDepArtifactId-messaging-common</artifactId>
                            <version>myDepVersion</version>
                        </dependency>
                    </dependencies>
                
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-jar-plugin</artifactId>
                                <configuration>
                                    <archive>
                                        <manifest>
                                            <addDefaultEntries>true</addDefaultEntries>
                                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                                            <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                                        </manifest>
                                        <manifestEntries>
                                            <Bundle-License>https://www.apache.org/licenses/LICENSE-2.0</Bundle-License>
                                        </manifestEntries>
                                    </archive>
                                </configuration>
                                <executions>
                                    <execution>
                                        <id>additional-artifact-with-classifier</id>
                                        <goals>
                                            <goal>jar</goal>
                                        </goals>
                                        <configuration>
                                            <classifier>myVersion</classifier>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        String dependency = PomFileGenerator.getCommonDependency("myDepGroupId", "myDepArtifactId", "myDepVersion");
        Path outputPath = Path.of("", "target", "unit-test-pom");
        File pomTemplateFile = new File("src/test/resources/pom-template/custom.pom.xml");
        PomFileGenerator pomFileGenerator = new PomFileGenerator(outputPath, pomTemplateFile, new SystemStreamLog());
        pomFileGenerator.generatePomFile("myGroupId", "myArtifactId", dependency, "myVersion", "jeapMessagingVersion");
        Path filename = Path.of(outputPath.toString(), "pom.xml");
        assertThat(Files.exists(filename))
                .isTrue();
        assertThat(Files.readString(filename))
                .isEqualToIgnoringWhitespace(expected);
    }
}
