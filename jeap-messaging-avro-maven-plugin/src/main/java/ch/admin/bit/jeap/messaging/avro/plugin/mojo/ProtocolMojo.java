package ch.admin.bit.jeap.messaging.avro.plugin.mojo;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.AvroCompiler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Generate Java classes and interfaces from Avro protocol files (.avpr)
 */
@Mojo(name = "protocol", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class ProtocolMojo extends AbstractAvroMojo {

    @Override
    protected String[] getIncludedFileEndings() {
        return new String[]{"avpr"};
    }

    @Override
    protected void compile(AvroCompiler avroCompiler, File file) throws MojoExecutionException {
        getLog().info("Compile protocol file " + file.getAbsolutePath());
        compileProtocol(avroCompiler, file);
    }
}
