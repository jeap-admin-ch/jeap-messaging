package ch.admin.bit.jeap.messaging.kafka.auth;

import java.util.Map;

public interface KafkaAuthProperties {

    Map<String, Object> authenticationProperties(String clusterName);
}
