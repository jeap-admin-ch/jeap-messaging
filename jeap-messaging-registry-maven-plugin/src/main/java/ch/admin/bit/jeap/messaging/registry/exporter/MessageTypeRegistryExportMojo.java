package ch.admin.bit.jeap.messaging.registry.exporter;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.IdlFileParser;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import org.apache.avro.Protocol;
import org.apache.avro.compiler.idl.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

@Mojo(name = "generateAvdl", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class MessageTypeRegistryExportMojo extends AbstractMojo {
    @SuppressWarnings("unused")
    @Parameter(name = "descriptorDirectory", defaultValue = "${basedir}/descriptor")
    private File descriptorDirectory;
    @SuppressWarnings("unused")
    @Parameter(name = "gitUrl")
    private String gitUrl;
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;
    @SuppressWarnings("unused")
    @Parameter(name = "targetDir", defaultValue = "${basedir}/target/avro")
    private File targetDir;

    @Override
    public void execute() {
        String[] filenames = descriptorDirectory.list();
        if (filenames == null) {
            return;
        }

        for (String filename : filenames) {
            if (MessageTypeRegistryConstants.COMMON_DIR_NAME.equals(filename)) {
                continue;
            }
            exportSystemDir(filename);
        }
    }


    private void exportSystemDir(String systemDirName) {
        File inputDir = new File(descriptorDirectory, systemDirName);
        String[] filenames = inputDir.list();
        if (filenames == null) {
            return;
        }

        for (String filename : filenames) {
            if (MessageTypeRegistryConstants.COMMON_DIR_NAME.equals(filename)) {
                continue;
            }
            exportSystemMsgTypeDir(systemDirName, filename);
        }
    }


    private void exportSystemMsgTypeDir(String systemDirName, String commandEventDirName) {
        String systemCommandEventDirName = systemDirName+'/'+commandEventDirName;
        File inputDir = new File(descriptorDirectory, systemCommandEventDirName );
        String[] filenames = inputDir.list();
        if (filenames == null) {
            return;
        }

        for (String filename : filenames) {
            if (MessageTypeRegistryConstants.COMMON_DIR_NAME.equals(filename)) {
                continue;
            }
            exportCommandOrEventDir(systemDirName, systemCommandEventDirName, filename);
        }
    }


    private void exportCommandOrEventDir(String systemDirName, String systemCommandEventDirName, String eventDirName) {
        // String systemEventDirName = systemDirName + '/' + EventRegistryConstants.EVENT_DIR_NAME;
        File systemEventDir = new File(descriptorDirectory, systemCommandEventDirName);
        File inputDir = new File(systemEventDir, eventDirName);
        File outputDir = new File(new File(targetDir, systemCommandEventDirName), eventDirName);
        try {
            FileUtils.forceMkdir(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output dir " + outputDir, e);
        }

        String[] filenames = inputDir.list();
        if (filenames == null) {
            return;
        }

        for (String filename : filenames) {
            File inputFile = new File(inputDir, filename);
            if (filename.toLowerCase().endsWith(".avpr") || filename.toLowerCase().endsWith(".avsc")) {
                File outputFile = new File(outputDir, filename);
                try {
                    FileUtils.copyFile(inputFile, outputFile);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot copy " + inputFile + " to " + outputFile, e);

                }
            }
            if (filename.toLowerCase().endsWith(".avdl")) {
                String newFilename = filename.substring(0, filename.length() - 5) + ".avpr";
                File outputFile = new File(outputDir, newFilename);
                File systemDir = new File(descriptorDirectory, systemDirName);

                try (ImportClassLoader importClassLoader = new ImportClassLoader(
                        new ImportClassLoader(descriptorDirectory, project.getRuntimeClasspathElements()),
                        new File(systemDir, MessageTypeRegistryConstants.COMMON_DIR_NAME),
                        new File(descriptorDirectory, MessageTypeRegistryConstants.COMMON_DIR_NAME))) {
                    IdlFileParser idlFileParser = new IdlFileParser(importClassLoader);
                    Protocol protocol = idlFileParser.parseIdlFile(inputFile);
                    FileUtils.write(outputFile, protocol.toString(true));
                } catch (DependencyResolutionRequiredException e) {
                    throw new RuntimeException("Cannot get runtime classpath", e);
                } catch (ParseException e) {
                    throw new RuntimeException("Cannot compile file " + inputFile, e);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot copy " + inputFile + " to " + outputFile, e);
                }
            }
        }
    }
}
