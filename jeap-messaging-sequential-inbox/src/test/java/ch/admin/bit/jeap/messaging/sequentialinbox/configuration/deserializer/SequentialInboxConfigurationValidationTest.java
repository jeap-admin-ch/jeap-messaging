package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.Sequence;
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
    @CsvSource(delimiterString = ";", value = {
            "deadlock-events.yml;Circular predecessor definition: MyEventType3 -> MyEventType6 -> MyEventType3",
            "deadlock-events-2.yml;Circular predecessor definition: MyEventType3 -> MyEventType6 -> MyEventType3",
            "deadlock-events-3.yml;Circular predecessor definition: MyEventType4 -> MyEventType5 -> MyEventType4",
            "duplicated-event.yml;Duplicated message types: [MyEventType1]",
            "duplicated-topic.yml;Duplicated topics: [test-topic-1]",
            "predecessor-event-missing.yml;Predecessor not found: MyEventType2",
            "contextIdExtractorClass-missing.yml;Error while parsing configuration file: classpath:/configurations/invalid/contextIdExtractorClass-missing.yml",
            "messageFilterClass-missing.yml;Error while parsing configuration file: classpath:/configurations/invalid/messageFilterClass-missing.yml",
            "empty-sequence.yml;The sequential inbox configuration contains an empty sequence (sequence name: eventType2AfterEventType1)",
            "missing-sequence-name.yml;The sequential inbox configuration contains a sequence without a type attribute",
            "missing-message-type.yml;The sequential inbox configuration contains a message without a type attribute in sequence test",
            "missing-context-id-extractor.yml;The sequential inbox configuration contains a message without a contextIdExtractor in sequence test",
            "missing-retention-period.yml;Missing required retention period for sequence name: eventType2AfterEventType1",
            "invalid-duration-format.yml;Error while parsing configuration file: classpath:/configurations/invalid/invalid-duration-format.yml",
            "duplicated-event-with-subtype.yml;Duplicated message types: [MyEventType1.BANANAS]",
            "unknown-subtype.yml;Unknown subtype for type MyEventType1. Configured subtypes: [UNKNOWN, APPLES, BANANAS]. Valid subtypes: [BANANAS, APPLES]",
            "missing-subtype.yml;Missing subtype for type MyEventType1. Configured subtypes: [BANANAS]. Valid subtypes: [BANANAS, APPLES]",
            "missing-subtype-predecessor.yml;Predecessor not found: MyEventType1.NOTEXISTS",
            "missing-subtype-resolver.yml;Missing subtype resolver for message type which has a subtype defined: MyEventType1",
            "different-topics-for-subtypes.yml;Different topics configured for subtypes of message type MyEventType1: [topic-1, topic-2]",
            "different-topics-for-subtype-with-default.yml;Different topics configured for subtypes of message type MyEventType1: [<default message type topic>, topic-1]",
            "subtype-resolver-for-existent-type.yml;Subtype resolver for message type that is either not sequenced or has not subtypes defined: DoesNotExistEvent",
            "subtype-resolver-for-type-without-subtype.yml;Subtype resolver for message type that is either not sequenced or has not subtypes defined: MyEventType1",
            "mixed-type-and-subtype.yml;Mix of configuration of messages type with and without subtype: [MyEventType1, MyEventType2]"
    })
    void testInvalidConfigurations(String filename, String exceptionMessage) {
        doTestWithInvalidConfiguration("classpath:/configurations/invalid/" + filename, exceptionMessage);
    }

    void doTest(String path) {
        SequentialInboxConfigurationLoader loader = new SequentialInboxConfigurationLoader(path);
        SequentialInboxConfiguration sequentialInboxConfiguration = loader.loadSequenceDeclaration();
        assertThat(sequentialInboxConfiguration)
                .isNotNull();
        assertThat(sequentialInboxConfiguration.getSequenceCount())
                .isPositive();
        sequentialInboxConfiguration.getSequencedMessageTypes().forEach(smt -> {
            Sequence sequence = sequentialInboxConfiguration.getSequenceByQualifiedSequencedMessageTypeName(smt.getQualifiedName());
            assertThat(sequence.getMessages()).isNotEmpty();
            sequence.getMessages().forEach(message -> {
                assertThat(message.getQualifiedName()).isNotNull();
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
