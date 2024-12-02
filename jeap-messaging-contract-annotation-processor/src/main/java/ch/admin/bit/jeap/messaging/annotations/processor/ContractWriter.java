package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.processor.appname.AppNameDetector;
import ch.admin.bit.jeap.messaging.contract.v2.Contract;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import static javax.tools.Diagnostic.Kind.ERROR;

class ContractWriter {
    private static final String CONTRACTS_PACKAGE = "ch.admin.bit.jeap.messaging.contracts";
    private static final String CONTRACT_VERSION = "1.0.0";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    private static final String MESSAGE_TYPE_NAME = "MESSAGE_TYPE_NAME";
    private static final String SYSTEM_NAME = "SYSTEM_NAME";
    private static final String MESSAGE_TYPE_VERSION = "MESSAGE_TYPE_VERSION";
    private static final String REGISTRY_URL = "REGISTRY_URL";
    private static final String REGISTRY_BRANCH = "REGISTRY_BRANCH";
    private static final String REGISTRY_COMMIT = "REGISTRY_COMMIT";
    private static final String COMPATIBILITY_MODE = "COMPATIBILITY_MODE";
    private static final String DEFAULT_TOPIC = "DEFAULT_TOPIC";
    public static final String CONTRACT_JSON = "-contract.json";

    static void generateContract(ProcessingEnvironment processingEnv, Element annotatedElement, TypeMirror typeMirror,
                                 ContractRole contractRole, String[] topics, String appNameFromAnnotation, String encryptionKeyId) {
        try {
            generateContractInternal(processingEnv, annotatedElement, typeMirror, contractRole, topics, appNameFromAnnotation, encryptionKeyId);
        } catch (MessagingAnnotationProcessingException ex) {
            processingEnv.getMessager().printMessage(ERROR, ex.getMessage(), annotatedElement);
            throw ex;
        }
    }

    private static void generateContractInternal(ProcessingEnvironment processingEnv, Element annotatedElement, TypeMirror typeMirror,
                                                 ContractRole contractRole, String[] topics, String appNameFromAnnotation, String encryptionKeyId) {

        TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);
        String messageTypeName = getAttributeValue(typeElement, MESSAGE_TYPE_NAME);
        String messageTypeVersion = getAttributeValue(typeElement, MESSAGE_TYPE_VERSION);
        AppNameDetector appNameDetector = new AppNameDetector(processingEnv);
        String appName = appNameDetector.detectAppName(annotatedElement, appNameFromAnnotation);
        String fileName = getFileName(appName, contractRole, messageTypeName, messageTypeVersion);
        FileObject contractFile = createResource(processingEnv, annotatedElement, fileName, messageTypeName);

        Contract contract = Contract.builder()
                .contractVersion(CONTRACT_VERSION)
                .role(contractRole.name().toLowerCase())
                .systemName(getAttributeValue(typeElement, SYSTEM_NAME))
                .messageTypeName(messageTypeName)
                .messageTypeVersion(messageTypeVersion)
                .registryUrl(getAttributeValue(typeElement, REGISTRY_URL))
                .registryBranch(getAttributeValue(typeElement, REGISTRY_BRANCH))
                .registryCommit(getAttributeValue(typeElement, REGISTRY_COMMIT))
                .compatibilityMode(getAttributeValue(typeElement, COMPATIBILITY_MODE))
                .appName(appName)
                .topics(getTopics(annotatedElement, topics, typeElement))
                .encryptionKeyId(encryptionKeyId)
                .build();

        try (PrintWriter writer = new PrintWriter(contractFile.openWriter())) {
            GSON.toJson(contract, writer);
        } catch (IOException e) {
            throw MessagingAnnotationProcessingException.processingFailed(e);
        }
    }

    private static FileObject createResource(ProcessingEnvironment processingEnv, Element annotatedElement,
                                             String fileName, String messageTypeName) {
        Filer filer = processingEnv.getFiler();
        try {
            return filer.createResource(StandardLocation.CLASS_OUTPUT, CONTRACTS_PACKAGE, fileName, annotatedElement);
        } catch (IOException e) {
            throw MessagingAnnotationProcessingException.duplicatedContract(annotatedElement, messageTypeName, fileName, e);
        }
    }

    private static String getFileName(String appName, ContractRole contractRole, String messageTypeName, String messageTypeVersion) {
        return appName + "-" + messageTypeName + "-" + messageTypeVersion + "." + contractRole.name().toLowerCase() + CONTRACT_JSON;
    }

    private static String[] getTopics(Element annotatedElement, String[] topics, TypeElement typeElement) {
        String defaultTopic = getOptionalAttributeValue(typeElement, DEFAULT_TOPIC).orElse(null);
        // Topics: If Topics in Annotation empty, insert the DefaultTopic from TypeRef
        if (topics.length > 0) {
            return topics;
        } else if (defaultTopic != null) {
            return new String[]{defaultTopic};
        } else {
            throw MessagingAnnotationProcessingException.missingTopic(annotatedElement, getAttributeValue(typeElement, MESSAGE_TYPE_NAME));
        }
    }

    private static String getAttributeValue(TypeElement typeElement, String name) {
        return getOptionalAttributeValue(typeElement, name).orElseThrow(() ->
                new IllegalArgumentException(("Missing field " + name + " on " + typeElement.getQualifiedName())));
    }

    private static Optional<String> getOptionalAttributeValue(TypeElement typeElement, String name) {
        List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
        return enclosedElements.stream()
                .filter(element -> name.equals(element.getSimpleName().toString()))
                .findFirst()
                .map(element -> (VariableElement) element)
                .filter(variableElement -> variableElement.getConstantValue() != null)
                .map(variableElement -> variableElement.getConstantValue().toString());
    }
}
