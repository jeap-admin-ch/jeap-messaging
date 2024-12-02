package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import net.logstash.logback.stacktrace.StackElementFilter;
import net.logstash.logback.stacktrace.StackHasher;

import java.util.List;
import java.util.regex.Pattern;

public class StackTraceHasher {

    private final StackHasher stackHasher;

    public StackTraceHasher(KafkaProperties kafkaProperties) {
        StackElementFilter filter = StackElementFilter.byPattern(
                toPattern(kafkaProperties.getErrorStackTraceHashExclusionPatterns()));
        stackHasher = new StackHasher(filter);
    }

    public String hash(Throwable throwable) {
        if (throwable != null) {
            return stackHasher.hexHash(throwable);
        } else {
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private List<Pattern> toPattern(List<String> patternStrings) {
        return patternStrings.stream().
                map(Pattern::compile).
                toList();
    }

}
