package ch.admin.bit.jeap.messaging.avro.plugin.git;

/**
 * Factory class to create instances of {@link ProcessBuilder}.
 * This allows for easier testing and mocking of process execution.
 */
public class ProcessBuilderFactory {

    ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }

}
