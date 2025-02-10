package ch.admin.bit.jeap.messaging.kafka.signature;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

public class SignatureMetricsService {

    private static final String VALIDITY_DAYS_REMAINING_METRIC_NAME = "jeap_messaging_signature_certificate_days_remaining";

    private final MeterRegistry meterRegistry;
    private final String applicationName;

    public SignatureMetricsService(MeterRegistry meterRegistry, @Value("${spring.application.name}") String applicationName) {
        this.meterRegistry = meterRegistry;
        this.applicationName = applicationName;
    }

    public void initCertificateValidityRemainigDays(Supplier<Number> validityRemainigDaysSupplier) {
        Gauge.builder(VALIDITY_DAYS_REMAINING_METRIC_NAME, validityRemainigDaysSupplier)
                .description("Number of days until certificate expires")
                .tag("application", applicationName)
                .register(meterRegistry);
    }
}