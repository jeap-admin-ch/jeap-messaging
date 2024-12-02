package ch.admin.bit.jeap.messaging.kafka.test.integration.multicluster;

import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class TestMessageConsumer<K,V> {

    private final ReactiveKafkaConsumerTemplate<K,V> consumerTemplate;
    private final Collection<String> subscriptionTopics;
    private final List<V> consumedMessages = new ArrayList<>();

    public TestMessageConsumer(ReceiverOptions<K,V> receiverOptions) {
        this.consumerTemplate = new ReactiveKafkaConsumerTemplate<>(receiverOptions);
        this.subscriptionTopics = receiverOptions.subscriptionTopics();
    }

    public void startConsuming() {
        consumerTemplate.receiveAutoAck().doOnNext(r -> consumedMessages.add(r.value())).subscribe();
        subscriptionTopics.forEach(this::waitForConsumerPartitionOnTopic);
    }

    public boolean hasConsumedMessages() {
        return !consumedMessages.isEmpty();
    }

    public List<V> getConsumedMessages() {
        return unmodifiableList(consumedMessages);
    }

    private void waitForConsumerPartitionOnTopic(String topic) {
        consumerTemplate.partitionsFromConsumerFor(topic).blockLast(Duration.ofSeconds(30));
    }

}
