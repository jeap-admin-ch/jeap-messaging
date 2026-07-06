package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.IdempotentProcessingJpaConfig;
import ch.admin.bit.jeap.messaging.idempotence.processing.jpa.SpringDataJpaIdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyHandlersConfig;
import ch.admin.bit.jeap.messaging.idempotence.testsupport.SleepyTestMessageHandler;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the concurrent execution contract test on an H2 database, on which the AUTO insert mode selects the portable
 * 'INSERT ... WHERE NOT EXISTS' insert.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = {IdempotentMessageHandlerConfig.class, IdempotentProcessingJpaConfig.class, SleepyHandlersConfig.class})
class JpaWhereNotExistsInsertConcurrentExecutionIT implements WhereNotExistsInsertConcurrentExecutionContract {

    @Getter
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private SpringDataJpaIdempotentProcessingRepository springDataJpaIdempotentProcessingRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Getter
    @Autowired
    private SleepyTestMessageHandler quickSleepyTestMessageHandler;

    @Getter
    @Autowired
    private SleepyTestMessageHandler slowSleepyTestMessageHandler;

    @Override
    public void inNewTransaction(Runnable runnable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        transactionTemplate.executeWithoutResult(_ -> runnable.run());
    }

}
