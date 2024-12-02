package ch.admin.bit.jeap.messaging.annotations.processor;

import javax.lang.model.element.Element;
import java.nio.file.Path;
import java.util.Set;

import static java.lang.String.format;

public class MessagingAnnotationProcessingException extends RuntimeException {
    private MessagingAnnotationProcessingException(String message) {
        super(message);
    }

    private MessagingAnnotationProcessingException(String message, Exception cause) {
        super(message, cause);
    }

    static MessagingAnnotationProcessingException missingTopic(Element annotatedElement, String messageTypeName) {
        return new MessagingAnnotationProcessingException(
                format("No default topic available in messaging contract on %s for message type %s. Please ",
                        annotatedElement, messageTypeName));
    }

    public static MessagingAnnotationProcessingException noAppNameFound(Element annotatedElement, Path projectRootDirectory) {
        return new MessagingAnnotationProcessingException(
                "No application name found for jEAP messaging contract " +
                "annotation on " + annotatedElement + ". " +
                "There must be either an appName attribute in the JeapMessage[Consumer/Producer]Contract annotation " +
                "(appName=\"the-app-name\") or a spring.application.name property in application.y[a]ml/bootstrap.y[a]ml. " +
                "Project root: " + projectRootDirectory);
    }

    public static MessagingAnnotationProcessingException projectDirectoryNotFound(Path annotatedSourceFile) {
        return new MessagingAnnotationProcessingException(
                "No application name found for jEAP messaging contract " +
                "annotation in file " + annotatedSourceFile + ". " +
                "There must be either an appName attribute in the JeapMessage[Consumer/Producer]Contract annotation " +
                "(appName=\"the-app-name\") or a spring.application.name property in application.y[a]ml/bootstrap.y[a]ml. " +
                "However, the annotation processor was unable to determine the root directory of the project. For this" +
                "functionality to work currently, a pom.xml file must be present in the module/project root directory.");
    }

    public static MessagingAnnotationProcessingException multipleAppNamesFound(Set<String> springApplicationNames,
                                                                               Element annotatedElement) {
        return new MessagingAnnotationProcessingException(
                "Multiple different application names " + springApplicationNames +
                " found for jEAP messaging contract annotation on " + annotatedElement + ". " +
                "There must be either an appName attribute in the JeapMessage[Consumer/Producer]Contract annotation " +
                "(appName=\"the-app-name\") or a spring.application.name property in application.y[a]ml/bootstrap.y[a]ml.");
    }

    static MessagingAnnotationProcessingException processingFailed(Exception cause) {
        return new MessagingAnnotationProcessingException("Failed to process jEAP messaging annotations",
                cause);
    }

    static MessagingAnnotationProcessingException duplicatedContract(Element annotatedElement,
                                                                     String messageTypeName,
                                                                     String fileName, Exception cause) {
        return new MessagingAnnotationProcessingException(
                "Failed to create contract file " + fileName + " for annotation on " +
                annotatedElement + " - most probably, this is due to a duplicate consumer/producer contract annotation for " +
                "the message type " + messageTypeName + " - please check if multiple contracts for this type are declared",
                cause);
    }

}
