package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DefaultContractsProvider implements ContractsProvider {

    private static final String CONTRACT_RESOURCES_LOCATION = "classpath*:/ch/admin/bit/jeap/messaging/contracts/";
    private static final String CONTRACT_RESOURCES_LOCATION_PATTERN = CONTRACT_RESOURCES_LOCATION + "*-contract.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final List<Contract> contracts;

    public DefaultContractsProvider() {
        this.contracts = readContracts();
        logContracts();
    }

    private void logContracts() {
        if (!contracts.isEmpty()) {
            log.debug("Found the following contracts: {}.", contracts.stream()
                    .map(this::toContractDescription)
                    .collect(Collectors.joining(", ")));
        } else {
            log.debug("No contracts present.");
        }
    }

    private String toContractDescription(Contract contract) {
        return String.format("%s-%s(role=%s, topics=%s, system=%s, app=%s)",
                contract.getMessageTypeName(), contract.getMessageTypeVersion(),
                contract.getRole(), toTopics(contract.getTopics()), contract.getSystemName(), contract.getAppName());
    }

    private String toTopics(String[] topics) {
        if (topics == null) {
            return "[]";
        } else {
            return "[" + String.join(", ", topics) + "]";
        }
    }

    @Override
    public List<Contract> getContracts() {
        return contracts;
    }

    private static List<Contract> readContracts() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] contractResources = resolver.getResources(CONTRACT_RESOURCES_LOCATION_PATTERN);
            log.debug("Number of contract resources found: {}.", contractResources.length);
            return Arrays.stream(contractResources).map(DefaultContractsProvider::readContract).toList();
        } catch (FileNotFoundException fnfe) {
            log.debug("Resource location '{}' for contracts not found, no v2 contracts loaded.", CONTRACT_RESOURCES_LOCATION);
            return Collections.emptyList();
        } catch (IOException ioe) {
            log.error("Loading contracts failed.", ioe);
            throw NoContractException.cannotReadContracts(CONTRACT_RESOURCES_LOCATION, ioe);
        }
    }

    private static Contract readContract(Resource contractResource) {
        try (InputStream contractResourceStream = contractResource.getInputStream()) {
            return OBJECT_MAPPER.readValue(contractResourceStream, Contract.class);
        } catch (IOException ioe) {
            throw NoContractException.cannotReadContractFile(contractResource, ioe);
        }
    }

}
