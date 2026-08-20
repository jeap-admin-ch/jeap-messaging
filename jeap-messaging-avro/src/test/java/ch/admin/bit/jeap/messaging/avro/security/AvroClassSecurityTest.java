package ch.admin.bit.jeap.messaging.avro.security;

import ch.admin.bit.jme.testsupport.NotAnAvroType;
import ch.admin.bit.jme.testsupportx.NotTrusted;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour of the whitelist itself. Which class is trusted under which configuration is covered by
 * {@link AvroClassSecurityMatrixTest}.
 */
class AvroClassSecurityTest {

    @BeforeEach
    void forgetInstalledWhitelist() {
        // The whitelist is installed once per JVM; these tests deliberately install different ones
        AvroClassSecurity.reset();
    }

    @AfterEach
    void restoreDefaultWhitelist() {
        AvroClassSecurity.reset();
        AvroClassSecurity.installDefaultIfMissing();
    }

    @Test
    void install_noPackagesAndNoClassesConfigured_trustsTheCuratedJdkTypes() {
        AvroClassSecurity.install(List.of(), List.of());

        assertTrue(isTrusted(java.util.ArrayList.class));
        assertTrue(isTrusted(java.util.LinkedList.class));
        assertTrue(isTrusted(java.util.HashMap.class));
        assertTrue(isTrusted(java.util.List.class));
        assertTrue(isTrusted(java.util.UUID.class));
        assertTrue(isTrusted(java.time.Instant.class));
        assertTrue(isTrusted(java.time.LocalDate.class));
        assertTrue(isTrusted(java.time.ZonedDateTime.class));
        assertTrue(isTrusted(java.util.Date.class));
        assertTrue(isTrusted(java.sql.Timestamp.class));
        assertTrue(isTrusted(String.class), "trusted by Avro itself");
        assertTrue(isTrusted(java.math.BigDecimal.class), "trusted by Avro itself");
        assertFalse(isTrusted(java.util.Properties.class));
        assertFalse(isTrusted(java.util.GregorianCalendar.class));
        assertFalse(isTrusted(java.time.format.DateTimeFormatter.class));
    }

    @Test
    void install_packageConfigured_matchesPackagesOnlyAndNotArbitraryPrefixes() {
        AvroClassSecurity.install(List.of("ch.admin.bit.jme.testsupport"), List.of());

        assertTrue(isTrusted(NotAnAvroType.class));
        assertFalse(isTrusted(NotTrusted.class), "ch.admin.bit.jme.testsupportx is a different package");
    }

    @Test
    void install_wildcardPackage_throwsException() {
        List<String> wildcard = List.of("*");

        assertThrows(IllegalArgumentException.class, () -> AvroClassSecurity.install(wildcard, List.of()));
    }

    @Test
    void forbiddenClass_namesTheJeapMessagingProperties() {
        AvroClassSecurity.install(List.of(), List.of());

        SecurityException exception = assertThrows(SecurityException.class,
                () -> ClassSecurityValidator.validate(java.util.Properties.class));

        assertTrue(exception.getMessage().contains("jeap.messaging.avro.trusted-packages"), exception.getMessage());
        assertTrue(exception.getMessage().contains("jeap.messaging.avro.trusted-classes"), exception.getMessage());
        assertTrue(exception.getMessage().contains("java.util.Properties"), exception.getMessage());
    }

    @Test
    void install_sameWhitelistTwice_isANoOp() {
        AvroClassSecurity.install(List.of("com.example.messaging"), List.of());

        assertDoesNotThrow(() -> AvroClassSecurity.install(List.of("com.example.messaging"), List.of()));
        assertTrue(isTrusted(com.example.messaging.ExternalPojo.class));
    }

    @Test
    void install_wildcardClass_throwsException() {
        List<String> wildcard = List.of("com.example.messaging.*");

        assertThrows(IllegalArgumentException.class, () -> AvroClassSecurity.install(List.of(), wildcard));
    }

    @Test
    void install_widerWhitelistAfterFirstInstall_isApplied() {
        AvroClassSecurity.install(List.of("com.example.messaging"), List.of());

        assertDoesNotThrow(() -> AvroClassSecurity.install(
                List.of("com.example.messaging", "com.example.other"), List.of()));
        assertTrue(isTrusted(com.example.messaging.ExternalPojo.class), "the original entry stays trusted");
    }

    @Test
    void install_afterInstallDefaultIfMissing_replacesTheProvisionalDefault() {
        AvroClassSecurity.installDefaultIfMissing();

        assertDoesNotThrow(() -> AvroClassSecurity.install(List.of("com.example.messaging"), List.of()));
        assertTrue(isTrusted(com.example.messaging.ExternalPojo.class));
    }

    @Test
    void install_differentWhitelistAfterFirstInstall_throwsAndKeepsTheInstalledOne() {
        AvroClassSecurity.install(List.of("com.example.messaging"), List.of());
        List<String> other = List.of("com.example.other");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> AvroClassSecurity.install(other, List.of()));

        assertTrue(exception.getMessage().contains("already installed"), exception.getMessage());
        assertTrue(exception.getMessage().contains("com.example.other"), exception.getMessage());
        assertTrue(isTrusted(com.example.messaging.ExternalPojo.class), "the installed whitelist stays in force");
    }

    @Test
    void installDefaultIfMissing_whitelistAlreadyInstalled_keepsInstalledWhitelist() {
        AvroClassSecurity.install(List.of("com.example.messaging"), List.of());

        AvroClassSecurity.installDefaultIfMissing();

        // Still the configured whitelist, the ch.admin default of installDefaultIfMissing() was not applied
        assertFalse(isTrusted(ch.admin.bit.jme.declaration.DeclarationReferences.class));
    }

    @Test
    void reset_restoresAvroDefaultWhitelist() {
        AvroClassSecurity.install(List.of(), List.of());

        AvroClassSecurity.reset();

        assertSame(ClassSecurityValidator.DEFAULT, ClassSecurityValidator.getGlobal());
    }

    private static boolean isTrusted(Class<?> clazz) {
        return ClassSecurityValidator.getGlobal().isTrusted(clazz);
    }
}
