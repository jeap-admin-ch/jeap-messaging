package ch.admin.bit.jeap.messaging.kafka.health;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdmin;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaClusterHealthIndicatorTest {

    @Mock
    private KafkaProperties kafkaProperties;
    @Mock
    private BeanFactory beanFactory;

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(100);
    // Zero grace period so failures are reported as DOWN immediately in tests
    private static final Duration IMMEDIATE_DOWN = Duration.ZERO;
    // Long grace period so failures are reported as recovering in tests
    private static final Duration LONG_GRACE_PERIOD = Duration.ofHours(1);

    @Test
    void health_whenBrokerUnreachable_returnsDown() {
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        when(kafkaProperties.clusterNames()).thenReturn(Set.of(KafkaProperties.DEFAULT_CLUSTER));
        when(beanFactory.getBean("kafkaAdmin", KafkaAdmin.class)).thenReturn(kafkaAdminWithUnreachableBroker());

        KafkaClusterHealthIndicator indicator = new KafkaClusterHealthIndicator(kafkaProperties, beanFactory, SHORT_TIMEOUT, IMMEDIATE_DOWN);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey(KafkaProperties.DEFAULT_CLUSTER);
        Health clusterHealth = (Health) health.getDetails().get(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(clusterHealth.getStatus()).isEqualTo(Status.DOWN);
        assertThat(clusterHealth.getDetails()).containsKey("failingSince");
    }

    @Test
    void health_whenBrokerUnreachableWithinGracePeriod_returnsUpRecovering() {
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        when(kafkaProperties.clusterNames()).thenReturn(Set.of(KafkaProperties.DEFAULT_CLUSTER));
        when(beanFactory.getBean("kafkaAdmin", KafkaAdmin.class)).thenReturn(kafkaAdminWithUnreachableBroker());

        KafkaClusterHealthIndicator indicator = new KafkaClusterHealthIndicator(kafkaProperties, beanFactory, SHORT_TIMEOUT, LONG_GRACE_PERIOD);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        Health clusterHealth = (Health) health.getDetails().get(KafkaProperties.DEFAULT_CLUSTER);
        assertThat(clusterHealth.getStatus()).isEqualTo(Status.UP);
        assertThat(clusterHealth.getDetails()).containsEntry("recovering", true);
        assertThat(clusterHealth.getDetails()).containsKey("failingSince");
    }

    @Test
    void health_whenOneClusterDown_overallStatusIsDown() {
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        when(kafkaProperties.clusterNames()).thenReturn(Set.of(KafkaProperties.DEFAULT_CLUSTER, "secondary"));
        when(beanFactory.getBean("kafkaAdmin", KafkaAdmin.class)).thenReturn(kafkaAdminWithUnreachableBroker());
        when(beanFactory.getBean("secondaryKafkaAdmin", KafkaAdmin.class)).thenReturn(kafkaAdminWithUnreachableBroker());

        KafkaClusterHealthIndicator indicator = new KafkaClusterHealthIndicator(kafkaProperties, beanFactory, SHORT_TIMEOUT, IMMEDIATE_DOWN);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKeys(KafkaProperties.DEFAULT_CLUSTER, "secondary");
        assertThat((Health) health.getDetails().get(KafkaProperties.DEFAULT_CLUSTER)).extracting(Health::getStatus).isEqualTo(Status.DOWN);
        assertThat((Health) health.getDetails().get("secondary")).extracting(Health::getStatus).isEqualTo(Status.DOWN);
    }

    @Test
    void health_whenInterrupted_resetsInterruptFlagAndReturnsDown() {
        when(kafkaProperties.getDefaultClusterName()).thenReturn(KafkaProperties.DEFAULT_CLUSTER);
        when(kafkaProperties.clusterNames()).thenReturn(Set.of(KafkaProperties.DEFAULT_CLUSTER));
        when(beanFactory.getBean("kafkaAdmin", KafkaAdmin.class)).thenReturn(kafkaAdminWithUnreachableBroker());

        KafkaClusterHealthIndicator indicator = new KafkaClusterHealthIndicator(kafkaProperties, beanFactory, SHORT_TIMEOUT, IMMEDIATE_DOWN);

        // Pre-interrupt so that Future.get() throws InterruptedException
        Thread.currentThread().interrupt();
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        // InterruptedException must be handled by re-interrupting the current thread
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted(); // clear flag so it does not affect other tests
    }

    private KafkaAdmin kafkaAdminWithUnreachableBroker() {
        return new KafkaAdmin(Map.of(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:1",
                CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "100",
                CommonClientConfigs.DEFAULT_API_TIMEOUT_MS_CONFIG, "100"
        ));
    }
}
