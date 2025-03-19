package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialInboxConfigurationLoaderTest {

    @Test
    void load() {
        SequentialInboxConfigurationLoader loader = new SequentialInboxConfigurationLoader(
                "classpath:/messaging/jeap-sequential-inbox.yml");
        SequentialInboxConfiguration sequentialInboxConfiguration = loader.loadSequenceDeclaration();
        assertThat(sequentialInboxConfiguration).isNotNull();
        assertThat(sequentialInboxConfiguration.getSequenceCount()).isEqualTo(2);

        SequencedMessageType smt = sequentialInboxConfiguration.requireSequencedMessageTypeByQualifiedName("MyEventType98");
        assertThat(smt.getClusterName()).isEqualTo("test-cluster");
        assertThat(smt.getContextIdExtractor()).isInstanceOf(TestContextIdExtractor.class);
        assertThat(smt.getMessageFilter()).isNull();
        Sequence seq = sequentialInboxConfiguration.getSequenceByQualifiedSequencedMessageTypeName("MyEventType98");
        assertThat(seq.getMessages().get(1).getMessageFilter())
                .isInstanceOf(TestMessageFilter.class);
        assertThat(seq.getMessages().get(1).getClusterName())
                .isNull();
        assertThat(seq.getRetentionPeriod())
                .isEqualTo(Duration.ofHours(6));
    }
}
