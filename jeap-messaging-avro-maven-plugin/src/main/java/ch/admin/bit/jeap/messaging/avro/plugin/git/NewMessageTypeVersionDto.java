package ch.admin.bit.jeap.messaging.avro.plugin.git;


import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeDescriptor;

import java.nio.file.Path;
import java.util.StringJoiner;

public record NewMessageTypeVersionDto(
        String systemName,
        Path descriptorPath,
        TypeDescriptor typeDescriptor,
        String version) {

    @Override
    public String toString() {
        return new StringJoiner(", ", NewMessageTypeVersionDto.class.getSimpleName() + "[", "]")
                .add("systemName='" + systemName + "'")
                .add("version='" + version + "'")
                .add("descriptorPath=" + descriptorPath)
                .toString();
    }
}
