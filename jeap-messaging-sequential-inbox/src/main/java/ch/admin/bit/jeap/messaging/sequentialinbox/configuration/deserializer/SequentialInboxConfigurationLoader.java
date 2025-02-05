package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.deserializer;

import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.ContextIdExtractor;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.MessageFilter;
import ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model.SequentialInboxConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("java:S1075")
@Slf4j
public class SequentialInboxConfigurationLoader {

    private static final String DEFAULT_CLASSPATH_LOCATION = "classpath:/messaging/jeap-sequential-inbox.yaml";

    private final String classpathLocation;

    public SequentialInboxConfigurationLoader() {
        this.classpathLocation = DEFAULT_CLASSPATH_LOCATION;
    }

    public SequentialInboxConfigurationLoader(String classpathLocation) {
        this.classpathLocation = classpathLocation;
    }

    public SequentialInboxConfiguration loadSequenceDeclaration() {
        log.info("Load SequentialInboxConfiguration from file {}", classpathLocation);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ContextIdExtractor.class, new ContextIdExtractorDeserializer());
        module.addDeserializer(MessageFilter.class, new MessageFilterDeserializer());
        mapper.registerModule(module);
        mapper.registerModule(new JavaTimeModule());
        SequentialInboxConfiguration sequentialInboxConfiguration = readSequentialInboxConfiguration(mapper);
        sequentialInboxConfiguration.validate();
        log.info("SequentialInboxConfiguration load with {} sequences", sequentialInboxConfiguration.getSequences().size());
        log.debug("SequentialInboxConfiguration loaded: {}", sequentialInboxConfiguration);
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

    private File loadConfigurationFile(){
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            return resolver.getResource(classpathLocation).getFile();
        } catch (IOException e) {
            log.error("Error while loading configuration file {}", classpathLocation, e);
            throw SequentialInboxConfigurationException.configurationFileLoadingError(classpathLocation, e);
        }
    }

}
