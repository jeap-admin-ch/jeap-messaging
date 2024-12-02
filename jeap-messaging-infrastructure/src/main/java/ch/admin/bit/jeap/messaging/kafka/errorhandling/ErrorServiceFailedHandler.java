package ch.admin.bit.jeap.messaging.kafka.errorhandling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;

@AutoConfiguration
@RequiredArgsConstructor
@Slf4j
public class ErrorServiceFailedHandler {
    private final ApplicationContext ctx;

    public void handle(Throwable e) {
        int exitCode = -1;
        log.error("Cannot connect to error service after an event handler has failed. " +
                "There is no safe way how this application can proceed, therefore shutting it down", e);

        //Let the spring application close itself
        SpringApplication.exit(ctx, () -> exitCode);

        //In case this has not worked shut down JVM
        log.error("Spring application did not shut down normally, forcefully close it", e);
        System.exit(exitCode);
    }
}
