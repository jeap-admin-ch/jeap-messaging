package ch.admin.bit.jeap.messaging.avro.plugin.mojo;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.AvroCompiler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

/**
 * Generate Java classes from Avro schema files (.avsc)
 */
@Mojo(name = "schema", requiresDependencyResolution = ResolutionScope.RUNTIME, defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class SchemaMojo extends AbstractAvroMojo {

    @Override
    protected String[] getIncludedFileEndings() {
        return new String[]{"avsc"};
    }

    @Override
    protected void compile(AvroCompiler avroCompiler, File file) throws MojoExecutionException {
        getLog().info("Compile schema file " + file.getAbsolutePath());
        compileSchema(avroCompiler, file);
    }
}
