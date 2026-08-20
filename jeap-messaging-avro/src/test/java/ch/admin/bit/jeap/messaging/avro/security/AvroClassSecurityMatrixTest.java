package ch.admin.bit.jeap.messaging.avro.security;

import ch.admin.bit.jeap.messaging.avro.AvroMessageUser;
import ch.admin.bit.jme.testsupport.NotAnAvroType;
import ch.admin.bit.jme.testsupportx.NotTrusted;
import com.example.messaging.*;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whitelist decision matrix, without a Spring context: every class kind against every kind of configuration.
 * <p>
 * The arms of the whitelist are independent: a class is trusted when Avro itself trusts it, when it is a JDK type
 * jEAP trusts, when it is an Avro generated type, or when the configuration names it - the last one regardless of
 * whether the class is Avro generated.
 */
class AvroClassSecurityMatrixTest {

    @BeforeEach
    void forgetInstalledWhitelist() {
        AvroClassSecurity.reset();
    }

    @AfterEach
    void restoreDefaultWhitelist() {
        AvroClassSecurity.reset();
        AvroClassSecurity.installDefaultIfMissing();
    }

    private static boolean isTrusted(Class<?> clazz) {
        return ClassSecurityValidator.getGlobal().isTrusted(clazz);
    }

    @Nested
    @DisplayName("no configuration: the ch.admin default applies")
    class DefaultWhitelist {

        @BeforeEach
        void install() {
            AvroClassSecurity.install(List.of(), List.of());
        }

        @Test
        void avroGeneratedTypesInsideTheDefaultPackageAreTrusted() {
            assertTrue(isTrusted(ch.admin.bit.jme.declaration.DeclarationReferences.class), "record in ch.admin");
            assertTrue(isTrusted(ch.admin.bit.jme.messaging.event.test.created.JmeTestCreatedEvent.class));
        }

        @Test
        void avroGeneratedTypesOutsideTheDefaultPackageAreNotTrusted() {
            // Being an Avro type narrows the trusted packages, it never trusts a class on its own - otherwise any
            // hand-written class could get itself trusted by implementing SpecificRecord
            assertFalse(isTrusted(ExternalRecord.class), "record outside ch.admin");
            assertFalse(isTrusted(ExternalEnum.class), "enum outside ch.admin");
            assertFalse(isTrusted(ExternalFixed.class), "fixed outside ch.admin");
        }

        @Test
        void jeapMessageTypesAreTrusted() {
            assertTrue(isTrusted(AvroMessageUser.class));
        }

        @Test
        void nonAvroClassesAreNotTrusted_notEvenInsideTheDefaultPackage() {
            assertFalse(isTrusted(NotAnAvroType.class), "non-Avro inside ch.admin");
            assertFalse(isTrusted(ExternalPojo.class), "non-Avro outside ch.admin");
        }

        @Test
        void jdkTypesFollowTheCuratedList() {
            assertTrue(isTrusted(java.util.ArrayList.class));
            assertTrue(isTrusted(java.util.UUID.class));
            assertTrue(isTrusted(java.time.Instant.class));
            assertTrue(isTrusted(String.class), "trusted by Avro itself");
            assertFalse(isTrusted(java.util.Properties.class));
            assertFalse(isTrusted(java.io.File.class));
        }
    }

    @Nested
    @DisplayName("trusted-packages configured: replaces the ch.admin default")
    class ConfiguredPackages {

        @BeforeEach
        void install() {
            AvroClassSecurity.install(List.of("com.example.messaging"), List.of());
        }

        @Test
        void nonAvroClassInTheConfiguredPackageIsTrusted() {
            assertTrue(isTrusted(ExternalPojo.class), "a configured package trusts non-Avro classes too");
            assertTrue(isTrusted(ExternalRecord.class), "and Avro types in it, of course");
            assertTrue(isTrusted(OtherExternalPojo.class), "the whole package, not a single class");
        }

        @Test
        void nonAvroClassInTheDefaultPackageIsNoLongerTrusted() {
            assertFalse(isTrusted(NotAnAvroType.class));
            assertFalse(isTrusted(NotTrusted.class));
        }

        @Test
        void jeapMessageTypesStayTrustedButApplicationAvroTypesDoNot() {
            assertTrue(isTrusted(AvroMessageUser.class), "jEAP Avro types are unconditional");
            assertFalse(isTrusted(ch.admin.bit.jme.declaration.DeclarationReferences.class),
                    "the ch.admin default is replaced, so application Avro types need to be configured");
        }

        @Test
        void jdkTypesStayTrusted() {
            assertTrue(isTrusted(java.util.ArrayList.class));
            assertTrue(isTrusted(java.time.Instant.class));
            assertFalse(isTrusted(java.util.Properties.class));
        }
    }

    @Nested
    @DisplayName("trusted-classes configured: exact class names, Avro generated or not")
    class ConfiguredClasses {

        @BeforeEach
        void install() {
            AvroClassSecurity.install(List.of(), List.of(ExternalPojo.class.getName()));
        }

        @Test
        void theConfiguredClassIsTrustedEvenThoughItIsNotAvroGenerated() {
            assertTrue(isTrusted(ExternalPojo.class));
        }

        @Test
        void aSiblingClassInTheSamePackageIsNotTrusted() {
            assertFalse(isTrusted(OtherExternalPojo.class), "a trusted class name is exact, not a prefix");
        }

        @Test
        void theDefaultPackageIsNoLongerTrustedForNonAvroClasses() {
            assertFalse(isTrusted(NotAnAvroType.class));
        }

        @Test
        void jeapMessageTypesStayTrusted() {
            assertTrue(isTrusted(AvroMessageUser.class), "jEAP Avro types are unconditional");
        }

        @Test
        void anAvroTypeOutsideAnyTrustedPackageIsStillRejected() {
            assertFalse(isTrusted(ExternalRecord.class));
        }
    }

    @Nested
    @DisplayName("trusted-packages and trusted-classes combined")
    class ConfiguredPackagesAndClasses {

        @BeforeEach
        void install() {
            AvroClassSecurity.install(List.of("ch.admin.bit.jme.testsupport"), List.of(ExternalPojo.class.getName()));
        }

        @Test
        void bothEntriesApply() {
            assertTrue(isTrusted(NotAnAvroType.class), "from the configured package");
            assertTrue(isTrusted(ExternalPojo.class), "from the configured class");
        }

        @Test
        void everythingElseNonAvroIsRejected() {
            assertFalse(isTrusted(NotTrusted.class), "sibling package is not covered");
            assertFalse(isTrusted(OtherExternalPojo.class), "sibling class is not covered");
        }
    }
}
