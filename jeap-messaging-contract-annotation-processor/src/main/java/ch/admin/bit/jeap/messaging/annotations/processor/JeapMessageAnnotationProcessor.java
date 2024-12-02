package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

import static ch.admin.bit.jeap.messaging.annotations.processor.ContractWriter.generateContract;

@SupportedAnnotationTypes({
        "ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract",
        "ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractContainer",
        "ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract",
        "ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContractContainer",
        "ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContracts",
        "ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContracts"})
public class JeapMessageAnnotationProcessor extends AbstractProcessor {

    private static final String[] USE_DEFAULT_TOPICS = new String[0];

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        // When more than one Annotation is given, they come inside the Container
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContractContainer.class)) {
            JeapMessageConsumerContractContainer messageConsumerContractContainer = annotatedElement.getAnnotation(JeapMessageConsumerContractContainer.class);
            for (JeapMessageConsumerContract jeapMessageConsumerContract : messageConsumerContractContainer.value()) {
                generateConsumerContract(annotatedElement, jeapMessageConsumerContract);
            }
        }
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageProducerContractContainer.class)) {
            JeapMessageProducerContractContainer messageProducerContractContainer = annotatedElement.getAnnotation(JeapMessageProducerContractContainer.class);
            for (JeapMessageProducerContract jeapMessageProducerContract : messageProducerContractContainer.value()) {
                generateProducerContract(annotatedElement, jeapMessageProducerContract);
            }
        }

        // Single annotations are returned directly
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContract.class)) {
            JeapMessageConsumerContract jeapMessageConsumerContract = annotatedElement.getAnnotation(JeapMessageConsumerContract.class);
            generateConsumerContract(annotatedElement, jeapMessageConsumerContract);
        }
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageProducerContract.class)) {
            JeapMessageProducerContract jeapMessageProducerContract = annotatedElement.getAnnotation(JeapMessageProducerContract.class);
            generateProducerContract(annotatedElement, jeapMessageProducerContract);
        }
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContracts.class)) {
            JeapMessageConsumerContracts jeapMessageConsumerContracts = annotatedElement.getAnnotation(JeapMessageConsumerContracts.class);
            generateConsumerContracts(annotatedElement, jeapMessageConsumerContracts);
        }
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageProducerContracts.class)) {
            JeapMessageProducerContracts jeapMessageProducerContracts = annotatedElement.getAnnotation(JeapMessageProducerContracts.class);
            generateProducerContracts(annotatedElement, jeapMessageProducerContracts);
        }

        return false;
    }

    private void generateConsumerContract(Element annotatedElement, JeapMessageConsumerContract jeapMessageConsumerContract) {
        TypeMirror typeMirror = AnnotationAttributes.getTypeMirrorForValueAttribute(jeapMessageConsumerContract);
        generateContract(processingEnv, annotatedElement, typeMirror, ContractRole.CONSUMER, jeapMessageConsumerContract.topic(), jeapMessageConsumerContract.appName(), null);
    }

    private void generateProducerContract(Element annotatedElement, JeapMessageProducerContract jeapMessageProducerContract) {
        TypeMirror typeMirror = AnnotationAttributes.getTypeMirrorForValueAttribute(jeapMessageProducerContract);
        generateContract(processingEnv, annotatedElement, typeMirror, ContractRole.PRODUCER, jeapMessageProducerContract.topic(), jeapMessageProducerContract.appName(),
                jeapMessageProducerContract.encryptionKeyId().isBlank() ? null : jeapMessageProducerContract.encryptionKeyId());
    }

    private void generateConsumerContracts(Element annotatedElement, JeapMessageConsumerContracts jeapMessageConsumerContracts) {
        List<TypeMirror> typeMirrors = AnnotationAttributes.getTypeMirrorsForValueAttribute(jeapMessageConsumerContracts);
        for (TypeMirror typeMirror : typeMirrors) {
            generateContract(processingEnv, annotatedElement, typeMirror, ContractRole.CONSUMER, USE_DEFAULT_TOPICS, jeapMessageConsumerContracts.appName(), null);
        }
    }

    private void generateProducerContracts(Element annotatedElement, JeapMessageProducerContracts jeapMessageProducerContracts) {
        List<TypeMirror> typeMirrors = AnnotationAttributes.getTypeMirrorsForValueAttribute(jeapMessageProducerContracts);
        for (TypeMirror typeMirror : typeMirrors) {
            generateContract(processingEnv, annotatedElement, typeMirror, ContractRole.PRODUCER, USE_DEFAULT_TOPICS, jeapMessageProducerContracts.appName(), null);
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
