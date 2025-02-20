package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RefreshScope
@RequiredArgsConstructor
@Slf4j
public class SubscriberValidationPropertiesContainer {

    private final SignatureSubscriberProperties signatureSubscriberProperties;
    private Map<String, AllowedPublishers> allowedPublishersByMessageName;

    @PostConstruct
    public void init() {
        this.allowedPublishersByMessageName = signatureSubscriberProperties.allowedPublishers().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new AllowedPublishers(entry.getKey(), entry.getValue())));
        log.info("SubscriberValidationPropertiesContainer initialized");
    }

    public boolean isPublisherAllowedForMessage(String messageType, String publisher) {
        AllowedPublishers allowedPublishers = allowedPublishersByMessageName.get(messageType);
        if (allowedPublishers == null) {
            return true;
        }
        return allowedPublishers.isAllowedForPublisher(publisher);
    }

    public boolean isSignatureRequired(String messageTypeName) {
        if (!isSignatureRequired()) {
            return false;
        }
        return !isMessageTypeWhitelisted(messageTypeName);
    }

    public boolean isSignatureRequired() {
        return signatureSubscriberProperties.requireSignature();
    }

    private boolean isMessageTypeWhitelisted(String messageType) {
        return signatureSubscriberProperties.acceptUnsignedMessagetypeWhitelist().contains(messageType);
    }

    private record AllowedPublishers(String event, List<String> publishers) {

        public boolean isAllowedForPublisher(String publisher) {
            return publishers.contains(publisher);
        }
    }
}
