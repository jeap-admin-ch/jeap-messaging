package ch.admin.bit.jeap.messaging.idempotence.messagehandler;

import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingIdentity;
import ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing.IdempotentProcessingRepository;
import ch.admin.bit.jeap.messaging.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandlerExecutionSkippedException.concurrentHandlingIdempotentProcessingException;
import static ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandlerExecutionSkippedException.generalIdempotentProcessingException;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentMessageHandlerAspect implements Ordered {

    private final IdempotentProcessingRepository idempotentProcessingRepository;
    private final IdempotentMessageHandlerConfig config;

    @Around("@annotation(IdempotentMessageHandler) && args(message,..)")
    public Object handleIdempotence(ProceedingJoinPoint joinPoint, Message message) throws Throwable {

        assertTransactionActive();

        String idempotenceId = message.getIdentity().getIdempotenceId();
        String idempotenceIdContext = message.getType().getName();
        IdempotentProcessingIdentity id = IdempotentProcessingIdentity.from(idempotenceId, idempotenceIdContext);

        log.debug("Checking for existing idempotent processing for {}.", id);
        if (createIdempotentProcessing(id)) {
            log.debug("No existing idempotent processing found for {}. Proceeding to processing.", id);
            return joinPoint.proceed();
        } else {
            log.debug("Existing idempotent processing found for {}. Skipping processing.", id);
            return null;
        }
    }

    private boolean createIdempotentProcessing(IdempotentProcessingIdentity idempotentProcessingIdentity) {
        try {
            return idempotentProcessingRepository.createIfNotExists(idempotentProcessingIdentity);
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException e) {
            throw concurrentHandlingIdempotentProcessingException(idempotentProcessingIdentity, e);
        } catch (Exception e) {
            throw generalIdempotentProcessingException(idempotentProcessingIdentity, e);
        }
    }

    @Override
    public int getOrder() {
        return config.getAdviceOrder();
    }

    private void assertTransactionActive() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Automatic idempotent message handling requires an active transaction!");
        }
    }
}
