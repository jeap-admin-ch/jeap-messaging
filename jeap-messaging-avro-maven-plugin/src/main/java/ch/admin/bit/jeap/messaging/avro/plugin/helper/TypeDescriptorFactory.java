package ch.admin.bit.jeap.messaging.avro.plugin.helper;

import ch.admin.bit.jeap.messaging.avro.plugin.registry.CommandDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.EventDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeDescriptor;
import org.apache.maven.plugin.MojoExecutionException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

public class TypeDescriptorFactory {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public static TypeDescriptor getTypeDescriptor(Path descriptor) throws MojoExecutionException {
        try {
            if (descriptor.getFileName().toString().endsWith("Event.json")) {
                return OBJECT_MAPPER.readValue(descriptor.toFile(), EventDescriptor.class);
            } else if (descriptor.getFileName().toString().endsWith("Command.json")) {
                return OBJECT_MAPPER.readValue(descriptor.toFile(), CommandDescriptor.class);
            } else {
                throw new MojoExecutionException("Unknown descriptor type: " + descriptor);
            }
        } catch (JacksonException e) {
            throw new MojoExecutionException("Cannot read value from json: " + e.getMessage(), e);
        }
    }

    public static TypeDescriptor readTypeDescriptor(String jsonContent, String descriptorPath) throws MojoExecutionException {
        if (jsonContent == null) {
            return null;
        }

        try {
            if (descriptorPath.endsWith("Event.json")) {
                return OBJECT_MAPPER.readValue(jsonContent, EventDescriptor.class);
            } else if (descriptorPath.endsWith("Command.json")) {
                return OBJECT_MAPPER.readValue(jsonContent, CommandDescriptor.class);
            } else {
                throw new MojoExecutionException("Unknown descriptor type: " + descriptorPath);
            }
        } catch (JacksonException e) {
            throw new MojoExecutionException("Cannot read value from json: " + e.getMessage(), e);
        }
    }
}
