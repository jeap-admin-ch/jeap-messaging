package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.StringUtils.hasText;

/**
 * Adds the name of the cluster as a header to received records. The {@link ErrorServiceSender} will use that information
 * to send a failed record to the correct cluster.
 */
@Slf4j
public class ClusterNameHeaderInterceptor implements ConsumerInterceptor<Object, Object> {

    public static final String CLUSTER_NAME_CONFIG = "clusternameheaderinterceptor.clustername";
    static final String CLUSTER_NAME_HEADER = "jeapClusterName";

    private byte[] clusterName;

    public static String getClusterName(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(CLUSTER_NAME_HEADER);
        if (header != null) {
            return new String(header.value(), UTF_8);
        }
        return null;
    }

    public static void addClusterName(Headers headers, String clusterName) {
        if (clusterName != null) {
            headers.add(new RecordHeader(CLUSTER_NAME_HEADER, clusterName.getBytes(UTF_8)));
        }
    }

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        records.forEach(r -> r.headers().add(CLUSTER_NAME_HEADER, clusterName));
        return records;
    }

    @Override
    public void configure(Map<String, ?> configs) {
        String clusterNameConfig = (String) configs.get(CLUSTER_NAME_CONFIG);
        if (!hasText(clusterNameConfig)) {
            throw new IllegalStateException("Mandatory config property %s is missing".formatted(CLUSTER_NAME_CONFIG));
        }
        this.clusterName = clusterNameConfig.getBytes(UTF_8);
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // nop
    }

    @Override
    public void close() {
        // nop
    }
}
