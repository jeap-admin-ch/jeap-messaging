package ch.admin.bit.jeap.messaging.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Annotation declares multiple message consumer contracts for an application by scanning
 * JSON template files for message definitions.
 * <p>
 * By default the files are read from {@code process/templates/*.json} or
 * {@code processarchive/*.json} under {@code src/[main|test]/resources}. Set {@link #templatesPath()} to scan a different
 * directory (e.g. {@code "opensearch"}).
 * <ul>
 * <li>
 *     appName: If not set, the 'spring.application.name' property in 'application.y[a]ml' or 'bootstrap.y[a]ml' will be used.
 * </li>
 * </ul>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface JeapMessageConsumerContractsByTemplates {

    String appName() default "";

    /**
     * Optional path to the directory containing the message template JSON files,
     * relative to {@code src/[main|test]/resources} (e.g. {@code "opensearch"}).
     * When blank (the default), the processor scans {@code process/templates} or
     * {@code processarchive} under {@code src/[main|test]/resources} as usual.
     */
    String templatesPath() default "";
}
