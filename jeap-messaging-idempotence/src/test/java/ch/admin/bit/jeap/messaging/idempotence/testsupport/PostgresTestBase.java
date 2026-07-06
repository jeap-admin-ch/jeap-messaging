package ch.admin.bit.jeap.messaging.idempotence.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need to run against a real PostgreSQL database, e.g. to verify the
 * PostgreSQL-specific 'INSERT ... ON CONFLICT DO NOTHING' idempotent processing insert. Tests extending this base
 * class are skipped if no docker environment is available to run the PostgreSQL testcontainer.
 * <p>
 * No {@code @AutoConfigureTestDatabase} configuration is needed in the extending tests: the default test database
 * replacement ({@code Replace.NON_TEST}) recognizes the DataSource provided by the {@code @ServiceConnection}
 * container as a test database and leaves it in place.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresTestBase {

    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("docker-hub.nexus.bit.admin.ch/postgres:17-alpine")
                    .asCompatibleSubstituteFor("postgres:17-alpine"));

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Runs the given action in a new transaction that commits when the action returns and rolls back when the action
     * throws. Tests extending this base class typically run without a test-managed transaction and therefore need to
     * demarcate transactions explicitly. Public so that this method can also serve as the implementation of a
     * transaction demarcation method required by a test contract interface.
     */
    public void inNewTransaction(Runnable runnable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate.executeWithoutResult(_ -> runnable.run());
    }

}
