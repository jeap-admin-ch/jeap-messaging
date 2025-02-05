package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.TestContextIdExtractor;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.TestMessageFilter;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SequentialInboxConfigurationLoaderTest {

    @Test
    void load() {
        SequentialInboxConfigurationLoader loader = new SequentialInboxConfigurationLoader();
        SequentialInboxConfiguration sequentialInboxConfiguration = loader.loadSequenceDeclaration();
        assertThat(sequentialInboxConfiguration).isNotNull();
        assertThat(sequentialInboxConfiguration.getSequences()).hasSize(2);

        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().getFirst().getType())
                .isEqualTo("MyEventType98");
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().getFirst().getClusterName())
                .isEqualTo("test-cluster");
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().getFirst().getMaxDelayPeriod())
                .isEqualTo(Duration.ofSeconds(1));
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().getFirst().getContextIdExtractor())
                .isInstanceOf(TestContextIdExtractor.class);
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().getFirst().getMessageFilter())
                .isNull();
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().get(1).getMessageFilter())
                .isInstanceOf(TestMessageFilter.class);
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().get(1).getMaxDelayPeriod())
                .isEqualTo(Duration.ofMinutes(150));
        assertThat(sequentialInboxConfiguration.getSequences().get(1).getMessages().get(1).getClusterName())
                .isNull();
    }


}