package ch.admin.bit.jeap.messaging.kafka.signature;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

public class SignatureMetricsService {

    private static final String VALIDITY_DAYS_REMAINING_METRIC_NAME = "jeap_messaging_signature_certificate_days_remaining";
    private static final String SIGNATURE_REQUIRED_METRIC_NAME = "jeap_messaging_signature_required_state";
    private static final String VALIDATION_OUTCOME_METRIC_NAME = "jeap_messaging_signature_validation_outcome";

    private final MeterRegistry meterRegistry;
    private final String applicationName;

    public SignatureMetricsService(MeterRegistry meterRegistry, @Value("${spring.application.name}") String applicationName) {
        this.meterRegistry = meterRegistry;
        this.applicationName = applicationName;
    }

    public void initCertificateValidityRemainingDays(Supplier<Number> validityRemainigDaysSupplier) {
        Gauge.builder(VALIDITY_DAYS_REMAINING_METRIC_NAME, validityRemainigDaysSupplier)
                .description("Number of days until certificate expires")
                .tag("application", applicationName)
                .register(meterRegistry);
    }

    public void initSignatureRequiredMetricName(Supplier<Number> signatureRequiredSupplier) {
        Gauge.builder(SIGNATURE_REQUIRED_METRIC_NAME, signatureRequiredSupplier)
                .description("Indicates if signature is required for messages (strict mode), 1 if required, 0 if not required")
                .tag("property", "jeap.messaging.authentication.subscriber.require-signature")
                .tag("application", applicationName)
                .register(meterRegistry);
    }

    public void recordSignatureValidation(String messageTypeName, boolean success) {
        Counter.builder(VALIDATION_OUTCOME_METRIC_NAME)
                .tag("messageType", messageTypeName)
                .tag("status", success ? "OK" : "NOK")
                .tag("application", applicationName)
                .description("Tracks signature validation outcomes per message type")
                .register(meterRegistry)
                .increment();
    }
}
