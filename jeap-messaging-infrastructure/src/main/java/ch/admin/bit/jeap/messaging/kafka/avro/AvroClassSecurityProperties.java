package ch.admin.bit.jeap.messaging.kafka.avro;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configures which classes Avro trusts to be referenced from a schema, see {@link AvroClassSecurity}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = AvroClassSecurityProperties.PREFIX)
public class AvroClassSecurityProperties {

    public static final String PREFIX = "jeap.messaging.avro";

    /**
     * Set to {@code false} to switch off {@link AvroClassSecurityAutoConfiguration}, for an application that installs
     * the whitelist itself.
     */
    public static final String AUTO_CONFIGURATION_ENABLED_PROPERTY = PREFIX + ".security-auto-configuration.enabled";

    /**
     * Packages whose classes are trusted to be referenced from an Avro schema, subpackages included. If neither
     * trusted packages nor trusted classes are configured, {@value AvroClassSecurity#DEFAULT_TRUSTED_PACKAGE} is
     * trusted, which covers the jEAP message types and the generated message types of admin.ch applications.
     */
    private List<String> trustedPackages = List.of();

    /**
     * Fully qualified names of individual classes trusted to be referenced from an Avro schema.
     */
    private List<String> trustedClasses = List.of();

    /**
     * Whether jEAP Messaging installs the Avro class whitelist on startup. Switch this off only if the application
     * installs it itself, before the first Avro (de)serialization - without a whitelist Avro rejects every generated
     * message class.
     */
    private SecurityAutoConfiguration securityAutoConfiguration = new SecurityAutoConfiguration();

    @Getter
    @Setter
    public static class SecurityAutoConfiguration {
        private boolean enabled = true;
    }
}
