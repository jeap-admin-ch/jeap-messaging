package ch.admin.bit.jeap.messaging.kafka;

import ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceFailedHandler;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.ErrorServiceSender;
import ch.admin.bit.jeap.messaging.kafka.errorhandling.StackTraceHasher;
import ch.admin.bit.jeap.messaging.kafka.filter.ErrorHandlingTargetFilter;
import ch.admin.bit.jeap.messaging.kafka.properties.KafkaProperties;
import ch.admin.bit.jeap.messaging.kafka.tracing.TracerBridge;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Optional;

@AutoConfiguration
@RequiredArgsConstructor
public class KafkaConsumerConfiguration {
    private final KafkaProperties properties;
    private final ErrorServiceFailedHandler errorServiceFailedHandler;
    private final Optional<TracerBridge> tracerBridge; // Only available if jeap-monitoring has been activated

    @Bean
    ErrorServiceSender errorServiceSender(BeanFactory beanFactory) {
        BackOff retrySendingError = new FixedBackOff(properties.getErrorServiceRetryIntervalMs(), properties.getErrorServiceRetryAttempts());
        TracerBridge tracerBridgeOrNull = tracerBridge.orElse(null);
        StackTraceHasher stackTraceHasher = new StackTraceHasher(properties);
        return new ErrorServiceSender(beanFactory, properties, errorServiceFailedHandler, retrySendingError, tracerBridgeOrNull, stackTraceHasher);
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    CommonErrorHandler errorHandler(ErrorServiceSender errorServiceSender) {
        BackOff noRetry = new FixedBackOff(0, 0);
        return new DefaultErrorHandler(errorServiceSender, noRetry);
    }

    @Bean
    ErrorHandlingTargetFilter wrongTargetMessageFilter(){
        return new ErrorHandlingTargetFilter(properties.getServiceName());
    }
}
