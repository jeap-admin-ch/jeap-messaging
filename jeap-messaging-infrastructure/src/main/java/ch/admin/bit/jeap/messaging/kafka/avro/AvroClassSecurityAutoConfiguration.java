package ch.admin.bit.jeap.messaging.kafka.avro;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Installs the Avro class whitelist required since Avro 1.12.2, see {@link AvroClassSecurity}.
 * <p>
 * Can be switched off with {@code jeap.messaging.avro.security-auto-configuration.enabled=false}, for an application
 * that installs the whitelist itself - it then has to do so before the first Avro (de)serialization.
 */
@AutoConfiguration
// The properties are bound manually below, the bean exists for the generated configuration metadata and the
// configprops actuator endpoint
@EnableConfigurationProperties(AvroClassSecurityProperties.class)
@ConditionalOnProperty(name = AvroClassSecurityProperties.AUTO_CONFIGURATION_ENABLED_PROPERTY, matchIfMissing = true)
@SuppressWarnings("java:S1118") // no private constructor, Spring instantiates this configuration class
public class AvroClassSecurityAutoConfiguration {

    /**
     * Installs the whitelist as a {@link BeanFactoryPostProcessor}, which runs before any other bean is created and
     * therefore before the first Avro (de)serialization can happen. The properties are bound directly from the
     * environment because the configuration properties bean does not exist yet at this point.
     */
    @Bean
    static BeanFactoryPostProcessor avroClassSecurityInstaller(Environment environment) {
        return beanFactory -> {
            AvroClassSecurityProperties properties = Binder.get(environment)
                    .bind(AvroClassSecurityProperties.PREFIX, AvroClassSecurityProperties.class)
                    .orElseGet(AvroClassSecurityProperties::new);
            AvroClassSecurity.install(properties.getTrustedPackages(), properties.getTrustedClasses());
        };
    }
}
