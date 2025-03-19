package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.ContextIdExtractor;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.MessageFilter;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SubTypeResolver;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@SuppressWarnings("java:S1075")
@Slf4j
public class SequentialInboxConfigurationLoader {

    private final String classpathLocation;

    public SequentialInboxConfigurationLoader(String classpathLocation) {
        this.classpathLocation = classpathLocation;
    }

    public SequentialInboxConfiguration loadSequenceDeclaration() {
        log.info("Load Sequential Inbox config from location {}", classpathLocation);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY); // prefer fields over getter/setter for json deserialization
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ContextIdExtractor.class, new ContextIdExtractorDeserializer());
        module.addDeserializer(SubTypeResolver.class, new SubTypeResolverDeserializer());
        module.addDeserializer(MessageFilter.class, new MessageFilterDeserializer());
        module.addDeserializer(Duration.class, new RetentionPeriodDeserializer());
        mapper.registerModule(module);
        SequentialInboxConfiguration sequentialInboxConfiguration = readSequentialInboxConfiguration(mapper);
        sequentialInboxConfiguration.validateAndInitialize();
        log.info("Sequential Inbox config loaded with {} sequences", sequentialInboxConfiguration.getSequenceCount());
        log.debug("Sequential Inbox config: {}", sequentialInboxConfiguration);
        return sequentialInboxConfiguration;
    }

    private SequentialInboxConfiguration readSequentialInboxConfiguration(ObjectMapper mapper) {
        try {
            return mapper.readValue(loadConfigurationFileInputStream(), SequentialInboxConfiguration.class);
        } catch (IOException e) {
            log.error("Error while reading configuration file {}", classpathLocation, e);
            throw SequentialInboxConfigurationException.configurationFileParsingError(classpathLocation, e);
        }
    }

    private InputStream loadConfigurationFileInputStream() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            return resolver.getResource(classpathLocation).getInputStream();
        } catch (IOException e) {
            log.error("Error while loading configuration file {}", classpathLocation, e);
            throw SequentialInboxConfigurationException.configurationFileLoadingError(classpathLocation, e);
        }
    }

}
