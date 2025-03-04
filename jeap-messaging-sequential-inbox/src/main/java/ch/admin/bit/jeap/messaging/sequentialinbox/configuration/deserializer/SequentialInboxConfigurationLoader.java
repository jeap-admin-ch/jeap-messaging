package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.ContextIdExtractor;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.MessageFilter;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
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
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ContextIdExtractor.class, new ContextIdExtractorDeserializer());
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
            return mapper.readValue(loadConfigurationFile(), SequentialInboxConfiguration.class);
        } catch (IOException e) {
            log.error("Error while reading configuration file {}", classpathLocation, e);
            throw SequentialInboxConfigurationException.configurationFileParsingError(classpathLocation, e);
        }
    }

    private File loadConfigurationFile() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            return resolver.getResource(classpathLocation).getFile();
        } catch (IOException e) {
            log.error("Error while loading configuration file {}", classpathLocation, e);
            throw SequentialInboxConfigurationException.configurationFileLoadingError(classpathLocation, e);
        }
    }

}
