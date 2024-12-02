package ch.admin.bit.jeap.messaging.avro.plugin.git;

import ch.admin.bit.jeap.messaging.avro.plugin.registry.EventDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeDescriptor;
import ch.admin.bit.jeap.messaging.avro.plugin.registry.TypeVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NewMessageTypeDiffTest {

    @Test
    void diffMessageTypes() {
        TypeDescriptor baseTypeDescriptor = createTypeDescriptor("0.1", "1.0");
        TypeDescriptor newTypeDescriptor = createTypeDescriptor("1.0", "2.0");
        Path descriptorPath = Path.of("descriptor/sys/event/fooevent/FooEvent.json");

        Path sourceDir = Path.of(".");
        Set<NewMessageTypeVersionDto> dto = NewMessageTypeDiff
                .diffMessageTypes(sourceDir, descriptorPath, newTypeDescriptor, baseTypeDescriptor);

        Path expectedPath = sourceDir.resolve(descriptorPath);
        assertThat(dto)
                .containsOnly(new NewMessageTypeVersionDto("sys", expectedPath, newTypeDescriptor, "2.0"));
    }

    private static TypeDescriptor createTypeDescriptor(String... versionNumbers) {
        List<TypeVersion> versions = Arrays.stream(versionNumbers)
                .map(v -> new TypeVersion(v, "schema.avdl", null, "NONE"))
                .toList();
        return new EventDescriptor(null, "type", null, versions, null, null);
    }
}
