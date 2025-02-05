package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequentialInboxConfigurationValidationTest {

    @Test
    void testValidConfigurations() {
        getValidFiles()
                .forEach(file -> doTest("classpath:/configurations/valid/" + file.getName()));
    }

    @ParameterizedTest
    @CsvSource({
            "deadlock-events.yaml,Circular predecessor definition: MyEventType3 -> MyEventType6 -> MyEventType3",
            "deadlock-events-2.yaml,Circular predecessor definition: MyEventType3 -> MyEventType6 -> MyEventType3",
            "deadlock-events-3.yaml,Circular predecessor definition: MyEventType4 -> MyEventType5 -> MyEventType4",
            "duplicated-event.yaml,Duplicated message types: [MyEventType1]",
            "duplicated-topic.yaml,Duplicated topics: [test-topic-1]",
            "predecessor-event-missing.yaml,Predecessor not found: MyEventType2",
            "contextIdExtractorClass-missing.yaml,Error while parsing configuration file: classpath:/configurations/invalid/contextIdExtractorClass-missing.yaml",
            "messageFilterClass-missing.yaml,Error while parsing configuration file: classpath:/configurations/invalid/messageFilterClass-missing.yaml",
    })
    void testInvalidConfigurations(String filename, String exceptionMessage) {
        doTestWithInvalidConfiguration("classpath:/configurations/invalid/" + filename, exceptionMessage);
    }

    void doTest(String path) {
        SequentialInboxConfigurationLoader loader = new SequentialInboxConfigurationLoader(path);
        SequentialInboxConfiguration sequentialInboxConfiguration = loader.loadSequenceDeclaration();
        assertThat(sequentialInboxConfiguration).isNotNull();
        assertThat(sequentialInboxConfiguration.getSequences()).isNotEmpty();
        sequentialInboxConfiguration.getSequences().forEach(sequence -> {
            assertThat(sequence.getMessages()).isNotEmpty();
            sequence.getMessages().forEach(message -> {
                assertThat(message.getType()).isNotNull();
                assertThat(message.getMaxDelayPeriod()).isNotNull();
                assertThat(message.getContextIdExtractor()).isNotNull();
            });
        });
    }

    void doTestWithInvalidConfiguration(String path, String exceptionMessage) {
        SequentialInboxConfigurationLoader loader = new SequentialInboxConfigurationLoader(path);
        Exception exception = assertThrows(SequentialInboxConfigurationException.class, loader::loadSequenceDeclaration);
        assertThat(exception.getMessage()).isEqualTo(exceptionMessage);

    }

    private Set<File> getValidFiles() {
        return Stream.of(Objects.requireNonNull(new File("src/test/resources/configurations/valid").listFiles()))
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toSet());
    }

}