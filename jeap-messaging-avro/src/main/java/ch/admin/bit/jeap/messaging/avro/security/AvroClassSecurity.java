package ch.admin.bit.jeap.messaging.avro.security;

import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.specific.SpecificFixed;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.ClassSecurityValidator;
import org.apache.avro.util.ClassSecurityValidator.ClassSecurityPredicate;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers the global Avro {@link ClassSecurityValidator} predicate that decides which classes are trusted to be
 * referenced from an Avro schema.
 * <p>
 * Since Avro 1.12.2 every class resolved from a schema - which includes <i>all</i> generated Avro record, enum and
 * fixed types, not only types referenced via the {@code java-class} property - has to be trusted explicitly.
 * Without a whitelist, Avro rejects the jEAP and application message classes with a {@link SecurityException}.
 * <p>
 * The predicate installed here trusts {@link ClassSecurityValidator#DEFAULT} (the hard-coded {@code java.lang} /
 * {@code java.math} types and whatever the {@code org.apache.avro.SERIALIZABLE_CLASSES} /
 * {@code org.apache.avro.SERIALIZABLE_PACKAGES} system properties allow), the JDK collection and value types
 * listed in {@link #TRUSTED_JDK_CLASSES}, the <i>Avro generated types</i> in {@value #JEAP_TRUSTED_PACKAGE} and
 * {@value #DEFAULT_TRUSTED_PACKAGE}, plus - whatever kind of class they are - the packages and classes passed to
 * {@link #install(Collection, Collection)}. If neither packages nor classes are given, the {@value #DEFAULT_TRUSTED_PACKAGE}
 * package is trusted, which covers the jEAP message types and the generated message types of admin.ch applications.
 * <p>
 * The whitelist is global, static state in Avro and is installed exactly once per JVM. In an application it is
 * installed by the {@code AvroClassSecurityAutoConfiguration} of the jeap-messaging starter, before any other bean
 * is created. Code running without a Spring context - tests of message builders, batch jobs, {@code main} methods -
 * installs it itself by calling {@link #installDefaultIfMissing()} before touching Avro.
 */
public final class AvroClassSecurity {

    /**
     * Package trusted if neither trusted packages nor trusted classes are configured.
     */
    public static final String DEFAULT_TRUSTED_PACKAGE = "ch.admin";

    /**
     * Package of the jEAP message base types, always trusted. Configuring trusted packages narrows
     * {@value #DEFAULT_TRUSTED_PACKAGE} down to the configured packages, but must never take the jEAP message types
     * themselves away - a service would otherwise lose `AvroMessageUser`, `AvroMessageType` or the error event types
     * by configuring a single package of its own.
     */
    public static final String JEAP_TRUSTED_PACKAGE = "ch.admin.bit.jeap";

    /**
     * Configuration properties extending the whitelist, named in the {@link SecurityException} of a rejected class.
     */
    private static final String TRUSTED_PACKAGES_PROPERTY = "jeap.messaging.avro.trusted-packages";
    private static final String TRUSTED_CLASSES_PROPERTY = "jeap.messaging.avro.trusted-classes";

    /**
     * JDK types that a schema may reference through the {@code java-class} / {@code java-key-class} property, always
     * trusted in addition to the configured packages and classes. They are collection and value types without any
     * side effects on loading or construction, which is why they are trusted unconditionally instead of having to be
     * whitelisted by every service using them.
     */
    public static final Set<String> TRUSTED_JDK_CLASSES = Set.of(
            // Collections
            "java.util.ArrayDeque",
            "java.util.ArrayList",
            "java.util.Collection",
            "java.util.Deque",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.util.LinkedHashMap",
            "java.util.LinkedHashSet",
            "java.util.LinkedList",
            "java.util.List",
            "java.util.Map",
            "java.util.NavigableMap",
            "java.util.NavigableSet",
            "java.util.Queue",
            "java.util.Set",
            "java.util.SortedMap",
            "java.util.SortedSet",
            "java.util.TreeMap",
            "java.util.TreeSet",
            "java.util.concurrent.ConcurrentHashMap",
            // Value types
            "java.util.UUID",
            "java.util.Date",
            "java.sql.Date",
            "java.sql.Time",
            "java.sql.Timestamp",
            "java.time.DayOfWeek",
            "java.time.Duration",
            "java.time.Instant",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.Month",
            "java.time.MonthDay",
            "java.time.OffsetDateTime",
            "java.time.OffsetTime",
            "java.time.Period",
            "java.time.Year",
            "java.time.YearMonth",
            "java.time.ZoneId",
            "java.time.ZoneOffset",
            "java.time.ZonedDateTime");

    private static final ClassSecurityPredicate TRUSTED_JDK_TYPES = clazz ->
            TRUSTED_JDK_CLASSES.contains(clazz.getName());

    /**
     * Recognizes the types the Avro compiler generates: records and error records ({@link SpecificRecord}), enums
     * ({@link GenericEnumSymbol}) and fixed types ({@link SpecificFixed}) - exactly the three schema kinds Avro
     * resolves from a schema.
     * <p>
     * This is a <b>narrowing</b> condition, never a reason to trust a class on its own: it is combined with the
     * built-in packages, so a class in {@value #DEFAULT_TRUSTED_PACKAGE} is trusted only when it is also an Avro
     * generated type. A schema therefore cannot name an entity, a Spring bean or any other class that happens to sit
     * in a built-in package - and cannot get a hand-written class trusted by having it implement
     * {@link SpecificRecord} either, because that class still has to live in a trusted package.
     * <p>
     * The check is safe on an un-initialized class: {@code isAssignableFrom} resolves the class hierarchy without
     * running static initializers, which is how Avro hands the class to the validator.
     */
    private static final ClassSecurityPredicate AVRO_GENERATED_TYPES = clazz ->
            SpecificRecord.class.isAssignableFrom(clazz)
            || GenericEnumSymbol.class.isAssignableFrom(clazz)
            || SpecificFixed.class.isAssignableFrom(clazz);

    private static TrustedNames installedPolicy;

    private AvroClassSecurity() {
    }

    /**
     * Installs the default whitelist unless one has already been installed. Intended for tests and for code running
     * without a Spring context, which does not know the application configuration.
     */
    public static synchronized void installDefaultIfMissing() {
        if (installedPolicy == null) {
            install(List.of(), List.of());
        }
    }

    /**
     * Installs the whitelist. The whitelist is installed <b>once</b> per JVM: installing the same packages and
     * classes again does nothing, installing a different set throws.
     * <p>
     * Re-installing cannot be supported: Avro caches the classes it resolved from a schema in
     * {@code SpecificData} and validates them only on a cache miss, so a whitelist installed after the first
     * (de)serialization would not revoke anything already resolved. Failing fast turns a narrowing that would only
     * be applied in part into an error at startup.
     * <p>
     * {@value #JEAP_TRUSTED_PACKAGE}, the JDK types of {@link #TRUSTED_JDK_CLASSES} and every Avro generated type
     * are trusted in any case.
     *
     * @param trustedPackages packages whose classes are trusted, including their subpackages. Empty means
     *                        {@value #DEFAULT_TRUSTED_PACKAGE}, unless trusted classes are given.
     * @param trustedClasses  fully qualified names of individual trusted classes
     * @throws IllegalStateException if a different whitelist has already been installed
     */
    public static synchronized void install(Collection<String> trustedPackages, Collection<String> trustedClasses) {
        Set<String> packagePrefixes = toPackagePrefixes(trustedPackages);
        Set<String> classNames = toNonBlankSet(trustedClasses);

        // The built-in packages only trust Avro generated types; jEAP's own package always, the wider default
        // package only as long as the service configures nothing itself
        Set<String> builtInPackagePrefixes = toPackagePrefixes(packagePrefixes.isEmpty() && classNames.isEmpty()
                ? List.of(JEAP_TRUSTED_PACKAGE, DEFAULT_TRUSTED_PACKAGE)
                : List.of(JEAP_TRUSTED_PACKAGE));

        TrustedNames policy = new TrustedNames(packagePrefixes, classNames, builtInPackagePrefixes);
        if (installedPolicy != null) {
            if (installedPolicy.equals(policy)) {
                return;
            }
            throw new IllegalStateException(("The Avro class whitelist is already installed with the packages %s and "
                    + "the classes %s and cannot be changed to the packages %s and the classes %s. Avro validates a "
                    + "class only the first time it resolves it, so a whitelist installed later would not apply to "
                    + "the classes already in use. Configure '%s' / '%s' before the application context starts.")
                    .formatted(sorted(installedPolicy.packagePrefixes()), sorted(installedPolicy.classNames()),
                            sorted(policy.packagePrefixes()), sorted(policy.classNames()),
                            TRUSTED_PACKAGES_PROPERTY, TRUSTED_CLASSES_PROPERTY));
        }

        ClassSecurityPredicate predicate = new JeapTrustedClasses(
                ClassSecurityValidator.composite(
                        ClassSecurityValidator.DEFAULT,
                        TRUSTED_JDK_TYPES,
                        policy),
                policy);
        ClassSecurityValidator.setGlobal(predicate);
        installedPolicy = policy;
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
    }


    /**
     * Restores Avro's own default whitelist and forgets the installed one, so that {@link #install(Collection,
     * Collection)} can install a different whitelist again.
     * <p>
     * <b>Intended for tests only.</b> Resetting does not undo anything: Avro keeps the classes it already resolved
     * in its {@code SpecificData} cache and does not validate them again.
     */
    public static synchronized void reset() {
        ClassSecurityValidator.setGlobal(ClassSecurityValidator.DEFAULT);
        installedPolicy = null;
    }

    private static Set<String> toPackagePrefixes(Collection<String> packages) {
        return toNonBlankSet(packages).stream()
                .map(AvroClassSecurity::toPackagePrefix)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String toPackagePrefix(String packageName) {
        if (packageName.contains("*")) {
            // Trusting all packages would defeat the purpose of the Avro class whitelist
            throw new IllegalArgumentException("Wildcards are not supported in trusted Avro packages: " + packageName);
        }
        // The trailing dot makes sure a package name is matched instead of an arbitrary prefix
        return packageName.endsWith(".") ? packageName : packageName + ".";
    }

    private static Set<String> toNonBlankSet(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * The configurable part of the whitelist.
     * <p>
     * A configured package or class is trusted whatever the class is - that is what makes the properties usable for
     * the non-Avro types a schema references through {@code java-class} / {@code java-key-class}. The built-in
     * packages are different: they trust a class only when it is <b>also</b> an Avro generated type, so they cannot
     * be used to reach an arbitrary class that happens to live under {@value #DEFAULT_TRUSTED_PACKAGE}.
     */
    private record TrustedNames(Set<String> packagePrefixes, Set<String> classNames,
                                Set<String> builtInPackagePrefixes) implements ClassSecurityPredicate {

        @Override
        public boolean isTrusted(Class<?> clazz) {
            String className = clazz.getName();
            return classNames.contains(className)
                   || startsWithAny(className, packagePrefixes)
                   || (AVRO_GENERATED_TYPES.isTrusted(clazz) && startsWithAny(className, builtInPackagePrefixes));
        }

        private static boolean startsWithAny(String className, Set<String> prefixes) {
            return prefixes.stream().anyMatch(className::startsWith);
        }
    }

    /**
     * Delegates the decision to the composed predicate, but rejects a class with a message naming the jEAP Messaging
     * properties instead of Avro's own message about the {@code org.apache.avro.SERIALIZABLE_*} system properties.
     */
    private record JeapTrustedClasses(ClassSecurityPredicate delegate, TrustedNames policy)
            implements ClassSecurityPredicate {

        @Override
        public boolean isTrusted(Class<?> clazz) {
            return delegate.isTrusted(clazz);
        }

        @Override
        public void forbiddenClass(String className) {
            throw new SecurityException(("Forbidden %s! This class is not trusted to be referenced from an Avro "
                    + "schema. Add its package to '%s' or the class itself to '%s'. Currently trusted are the "
                    + "packages %s and the classes %s (any class), the Avro generated types in %s, the JDK types of "
                    + "AvroClassSecurity.TRUSTED_JDK_CLASSES and Avro's own defaults.")
                    .formatted(className, TRUSTED_PACKAGES_PROPERTY, TRUSTED_CLASSES_PROPERTY,
                            sorted(policy.packagePrefixes()), sorted(policy.classNames()),
                            sorted(policy.builtInPackagePrefixes())));
        }
    }
}
