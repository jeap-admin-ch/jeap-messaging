package ch.admin.bit.jeap.messaging.kafka.avro;

import ch.admin.bit.jeap.messaging.avro.AvroMessageUser;
import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jme.testsupport.NotAnAvroType;
import com.example.messaging.ExternalPojo;
import com.example.messaging.ExternalRecord;
import com.example.messaging.OtherExternalPojo;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whitelist decision matrix as installed by the auto-configuration, i.e. through the
 * {@code jeap.messaging.avro.*} properties rather than through a direct {@code AvroClassSecurity.install(..)}.
 */
class AvroClassSecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AvroClassSecurityAutoConfiguration.class));

    @BeforeEach
    void forgetInstalledWhitelist() {
        // The whitelist is installed once per JVM; each context here installs a different one
        AvroClassSecurity.reset();
    }

    @AfterEach
    void restoreDefaultWhitelist() {
        AvroClassSecurity.reset();
        AvroClassSecurity.installDefaultIfMissing();
    }

    @Test
    void noPropertiesConfigured_installsTheDefaultWhitelist() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(isTrusted(AvroMessageUser.class)).as("jEAP message type").isTrue();
            assertThat(isTrusted(ExternalRecord.class)).as("Avro type outside ch.admin").isFalse();
            assertThat(isTrusted(NotAnAvroType.class)).as("non-Avro inside ch.admin").isFalse();
            assertThat(isTrusted(ExternalPojo.class)).as("non-Avro outside ch.admin").isFalse();
            assertThat(isTrusted(java.io.File.class)).as("untrusted JDK type").isFalse();
        });
    }

    @Test
    void trustedPackagesConfigured_trustsThatPackageAndDropsTheChAdminDefault() {
        contextRunner
                .withPropertyValues("jeap.messaging.avro.trusted-packages=com.example.messaging")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(isTrusted(ExternalPojo.class)).as("non-Avro in the configured package").isTrue();
                    assertThat(isTrusted(OtherExternalPojo.class)).as("whole package is trusted").isTrue();
                    assertThat(isTrusted(NotAnAvroType.class)).as("ch.admin default dropped").isFalse();
                    assertThat(isTrusted(AvroMessageUser.class)).as("jEAP Avro types unconditional").isTrue();
                    assertThat(isTrusted(ExternalRecord.class)).as("Avro type in the configured package").isTrue();
                    assertThat(isTrusted(java.util.ArrayList.class)).as("JDK types stay trusted").isTrue();
                });
    }

    @Test
    void trustedClassesConfigured_trustsExactlyThatClass() {
        contextRunner
                .withPropertyValues("jeap.messaging.avro.trusted-classes=" + ExternalPojo.class.getName())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(isTrusted(ExternalPojo.class)).as("configured non-Avro class").isTrue();
                    assertThat(isTrusted(OtherExternalPojo.class)).as("sibling class not covered").isFalse();
                    assertThat(isTrusted(NotAnAvroType.class)).as("ch.admin default dropped").isFalse();
                    assertThat(isTrusted(AvroMessageUser.class)).as("jEAP Avro types unconditional").isTrue();
                    assertThat(isTrusted(ExternalRecord.class))
                            .as("an Avro type outside any trusted package is still rejected").isFalse();
                });
    }

    @Test
    void trustedPackagesAndClassesConfigured_bothApply() {
        contextRunner
                .withPropertyValues(
                        "jeap.messaging.avro.trusted-packages=ch.admin.bit.jme.testsupport",
                        "jeap.messaging.avro.trusted-classes=" + ExternalPojo.class.getName())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(isTrusted(NotAnAvroType.class)).as("from the configured package").isTrue();
                    assertThat(isTrusted(ExternalPojo.class)).as("from the configured class").isTrue();
                    assertThat(isTrusted(OtherExternalPojo.class)).as("neither configured").isFalse();
                });
    }

    @Test
    void autoConfigurationDisabled_installsNothing() {
        contextRunner
                .withPropertyValues("jeap.messaging.avro.security-auto-configuration.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AvroClassSecurityAutoConfiguration.class);
                    assertThat(ClassSecurityValidator.getGlobal())
                            .as("no whitelist installed, Avro's own default is still in place")
                            .isSameAs(ClassSecurityValidator.DEFAULT);
                });
    }

    @Test
    void autoConfigurationExplicitlyEnabled_installsTheWhitelist() {
        contextRunner
                .withPropertyValues("jeap.messaging.avro.security-auto-configuration.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(isTrusted(AvroMessageUser.class)).isTrue();
                });
    }

    @Test
    void secondContextNarrowingTheWhitelist_failsFastInsteadOfApplyingItPartially() {
        contextRunner
                .withPropertyValues("jeap.messaging.avro.trusted-packages=com.example.messaging,com.example.other")
                .run(context -> assertThat(context).hasNotFailed());

        contextRunner
                .withPropertyValues("jeap.messaging.avro.trusted-packages=com.example.other")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("cannot be narrowed")
                        .hasMessageContaining("com.example.other"));
    }

    private static boolean isTrusted(Class<?> clazz) {
        return ClassSecurityValidator.getGlobal().isTrusted(clazz);
    }
}
