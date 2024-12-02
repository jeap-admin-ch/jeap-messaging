package ch.admin.bit.jeap.messaging.avro.plugin.mojo;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.AvroCompiler;
import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;

/**
 * Generate Java classes and interfaces from AvroIDL files (.avdl)
 */
@Mojo(name = "idl", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class IDLProtocolMojo extends AbstractAvroMojo {

    @Override
    protected String[] getIncludedFileEndings() {
        return new String[]{"avdl"};
    }

    @Override
    protected void compile(AvroCompiler avroCompiler, File file) throws MojoExecutionException {
        getLog().info("Compile IDL file " + file.getAbsolutePath());
        try (ImportClassLoader importClassLoader = new ImportClassLoader(getSourceDirectory(), getProject().getRuntimeClasspathElements())) {
            compileIdl(avroCompiler, file, importClassLoader);
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot create classpath loader", e);
        }
    }
}
