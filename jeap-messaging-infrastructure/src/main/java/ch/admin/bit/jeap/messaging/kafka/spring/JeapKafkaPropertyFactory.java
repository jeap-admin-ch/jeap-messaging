package ch.admin.bit.jeap.messaging.kafka.spring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.Set;

@RequiredArgsConstructor
public class JeapKafkaPropertyFactory {

    private final Environment environment;
    private final BeanFactory beanFactory;

    @Getter
    private KafkaProperties springKafkaProperties;
    @Getter
    private ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties jeapKafkaProperties;

    public static JeapKafkaPropertyFactory create(Environment environment, BeanFactory beanFactory) {
        JeapKafkaPropertyFactory jeapKafkaPropertyFactory = new JeapKafkaPropertyFactory(environment, beanFactory);
        jeapKafkaPropertyFactory.initialize();
        return jeapKafkaPropertyFactory;
    }

    private KafkaProperties createSpringKafkaProperties() {
        return Binder.get(environment)
                .bindOrCreate("spring.kafka", KafkaProperties.class);
    }

    private ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties createJeapKafkaProperties() {
        return createJeapKafkaProperties(environment);
    }

    public static ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties createJeapKafkaProperties(Environment environment) {
        ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties props = Binder.get(environment)
                .bindOrCreate("jeap.messaging.kafka", ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties.class);
        props.afterPropertiesSet();
        return props;
    }

    public Set<String> getClusterNames() {
        return jeapKafkaProperties.clusterNames();
    }

    private void initialize() {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            // Add embedded kafka defaults with low precedence if EmbeddedKafka is active
            String propertyValue = environment.getProperty("jeap.messaging.kafka.embedded");
            boolean embeddedKafkaPropsDisabled = "false".equals(propertyValue);
            if (!embeddedKafkaPropsDisabled) {
                boolean isEmbeddedKafkaPropertyTrue = Boolean.parseBoolean(propertyValue);
                if (beanFactory.containsBean("embeddedKafka") || isEmbeddedKafkaPropertyTrue) {
                    // Embedded kafka infrastructure properties (broker / schema registry address and auth) are added
                    // with high priority
                    PropertySource<?> embeddedKafkaPropertySource = loadDefaults("jeap-messaging-embedded-kafka.properties");
                    addHighPriorityPropertySource(configurableEnvironment, embeddedKafkaPropertySource);
                    // Non-infrastructure properties are added with low priority and can be overriden
                    PropertySource<?> embeddedKafkaDefaultsPropertySource = loadDefaults("jeap-messaging-embedded-kafka-defaults.properties");
                    addLowPriorityPropertySource(configurableEnvironment, embeddedKafkaDefaultsPropertySource);
                }
            }

            // Add jeap messaging default properties with the lowest precedence to be able to override them
            PropertySource<?> jeapKafkaPropSource = loadDefaults("jeap-messaging-kafka-defaults.properties");
            addLowPriorityPropertySource(configurableEnvironment, jeapKafkaPropSource);
        }
        this.springKafkaProperties = createSpringKafkaProperties();
        this.jeapKafkaProperties = createJeapKafkaProperties();
    }

    private static void addHighPriorityPropertySource(ConfigurableEnvironment configurableEnvironment, PropertySource<?> source) {
        // Add property source before properties attributes on tests to be able to override specific properties for tests as required
        // https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config
        if (!configurableEnvironment.getPropertySources().contains(source.getName())) {
            configurableEnvironment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, source);
        }
    }

    private static void addLowPriorityPropertySource(ConfigurableEnvironment configurableEnvironment, PropertySource<?> source) {
        if (!configurableEnvironment.getPropertySources().contains(source.getName())) {
            configurableEnvironment.getPropertySources().addLast(source);
        }
    }

    @SneakyThrows
    private static PropertySource<?> loadDefaults(String path) {
        return new PropertiesPropertySourceLoader()
                .load(path, new ClassPathResource(path))
                .get(0);
    }
}
