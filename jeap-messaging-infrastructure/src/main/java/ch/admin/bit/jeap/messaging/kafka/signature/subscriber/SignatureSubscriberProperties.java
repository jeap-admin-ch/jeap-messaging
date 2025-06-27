package ch.admin.bit.jeap.messaging.kafka.signature.subscriber;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.List;
import java.util.Map;
import java.util.Set;


@ConfigurationProperties(
        prefix = "jeap.messaging.authentication.subscriber"
)
@RefreshScope
public class SignatureSubscriberProperties {

    private static final Set<String> DEFAULT_PRIVILEGED_PRODUCER_COMMON_NAMES = Set.of("mirrormaker");

    private final boolean requireSignature;
    private final Set<String> acceptUnsignedMessagetypeWhitelist;
    private final Set<String> privilegedProducerCommonNames;
    private final Map<String, List<String>> allowedPublishers;
    private final Map<String, Map<String, List<String>>> certificateChains;

    /**
     * Configuration properties for the subscriber side of the message signing.
     *
     * @param requireSignature                   is a signature required? If not a message from service x is not signed and service x is not in the whitelist, the message will be rejected.
     * @param acceptUnsignedMessagetypeWhitelist if a signature is required, but producer services don't
     * @param allowedPublishers                  a list of Message types and services that are allowed to publish them.
     * @param certificateChains                  a list of certificate chains (full chains) of the producing services, last certificate in a chain must be the root, the first the leaf certificate
     */
    public SignatureSubscriberProperties(@DefaultValue("false") boolean requireSignature,
                                         Set<String> acceptUnsignedMessagetypeWhitelist,
                                         Set<String> privilegedProducerCommonNames,
                                         Map<String, List<String>> allowedPublishers,
                                         Map<String, Map<String, List<String>>> certificateChains) {
        this.requireSignature = requireSignature;
        this.acceptUnsignedMessagetypeWhitelist = acceptUnsignedMessagetypeWhitelist == null ? Set.of() : acceptUnsignedMessagetypeWhitelist;
        this.privilegedProducerCommonNames = privilegedProducerCommonNames == null ? DEFAULT_PRIVILEGED_PRODUCER_COMMON_NAMES : privilegedProducerCommonNames;
        this.allowedPublishers = allowedPublishers == null ? Map.of() : allowedPublishers;
        this.certificateChains = certificateChains == null ? Map.of() : certificateChains;
    }

    public boolean requireSignature() {
        return requireSignature;
    }

    public Set<String> acceptUnsignedMessagetypeWhitelist() {
        return acceptUnsignedMessagetypeWhitelist;
    }

    public Set<String> privilegedProducerCommonNames() {
        return privilegedProducerCommonNames;
    }

    public Map<String, List<String>> allowedPublishers() {
        return allowedPublishers;
    }

    public Map<String, Map<String, List<String>>> certificateChains() {
        return certificateChains;
    }
}
