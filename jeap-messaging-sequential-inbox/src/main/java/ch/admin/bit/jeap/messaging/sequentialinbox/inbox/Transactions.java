package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class Transactions {
    private static final TransactionDefinition REQUIRES_NEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    private static final TransactionDefinition NOT_SUPPORTED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

    private final PlatformTransactionManager platformTransactionManager;

    <T> T callInSuspendedTransaction(Supplier<T> callable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager, NOT_SUPPORTED);
        return transactionTemplate.execute(ignored -> callable.get());
    }

    public void runInNewTransaction(Runnable runnable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager, REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(ignored -> runnable.run());
    }

    <T> T callInNewTransaction(Supplier<T> callable) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager, REQUIRES_NEW);
        return transactionTemplate.execute(ignored -> callable.get());
    }

}
