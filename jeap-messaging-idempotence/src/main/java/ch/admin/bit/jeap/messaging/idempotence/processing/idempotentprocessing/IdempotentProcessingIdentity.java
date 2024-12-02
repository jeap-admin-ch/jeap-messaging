package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings("JpaDataSourceORMInspection")
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@Embeddable
@Slf4j
public class IdempotentProcessingIdentity implements Serializable {

    private static final Pattern MESSAGE_CONTEXT_PATTERN = Pattern.compile("^(.*)(V[0-9]+)(Event|Command)");

    @Column(name = "idempotence_id")
    private String id;

    @Column(name = "idempotence_id_context")
    private String context;

    public static IdempotentProcessingIdentity from(String id, String context) {
        Objects.requireNonNull(id, "Idempotence id must not be null.");
        Objects.requireNonNull(id, "Idempotence id context must not be null.");
        return new IdempotentProcessingIdentity(id, removeVersionFromContext(context));
    }

    private static String removeVersionFromContext(String context) {
        final var matcher = MESSAGE_CONTEXT_PATTERN.matcher(context);
        if (matcher.find()) {
            final var contextWithoutVersion = matcher.group(1) + matcher.group(3);
            log.debug("Found version in message context '{}'. Returning context without version '{}'...", matcher.group(0), contextWithoutVersion);
            return contextWithoutVersion;
        }
        return context;
    }

}
