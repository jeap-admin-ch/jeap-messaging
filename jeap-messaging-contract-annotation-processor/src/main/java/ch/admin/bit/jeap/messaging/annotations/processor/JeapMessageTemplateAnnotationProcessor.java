package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractsByTemplates;
import ch.admin.bit.jeap.messaging.annotations.processor.util.AvroClassFinder;
import ch.admin.bit.jeap.messaging.annotations.processor.util.TemplateMessageCollector;
import ch.admin.bit.jeap.messaging.annotations.processor.util.TemplatePathResolver;
import ch.admin.bit.jeap.messaging.annotations.processor.util.TypeRefFinder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.admin.bit.jeap.messaging.annotations.processor.ContractWriter.generateContract;

@SupportedAnnotationTypes("ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContractsByTemplates")
public class JeapMessageTemplateAnnotationProcessor extends AbstractProcessor {

    private static boolean alreadyProcessed = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // The processor is triggered for each maven module, ensuring processing is done only once.
        if (alreadyProcessed) {
            return false;
        }
        alreadyProcessed = true;

        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(JeapMessageConsumerContractsByTemplates.class)) {
            AvroClassFinder avroClassFinder = new AvroClassFinder(processingEnv.getMessager());
            Set<Class<?>> annotatedClasses = avroClassFinder.getAvroGeneratedClasses();

            TemplatePathResolver templatePathResolver = new TemplatePathResolver(processingEnv);
            String templatePath = templatePathResolver.getTemplatePath(annotatedElement);

            if (templatePath != null && !annotatedClasses.isEmpty()) {
                TemplateMessageCollector collector = new TemplateMessageCollector();
                Map<String, List<String>> templateMessages = collector.collectTemplateMessages(templatePath, processingEnv.getMessager());

                if (!templateMessages.isEmpty()) {
                    generateConsumerContracts(annotatedElement, annotatedClasses, templateMessages);
                }
            }
        }
        return true;
    }

    private void generateConsumerContracts(Element annotatedElement, Set<Class<?>> annotatedClasses, Map<String, List<String>> templateMessages) {
        String appName = annotatedElement.getAnnotation(JeapMessageConsumerContractsByTemplates.class).appName();

        for (Map.Entry<String, List<String>> entry : templateMessages.entrySet()) {
            String name = entry.getKey();
            List<String> topics = entry.getValue();
            TypeMirror typeMirror = TypeRefFinder.findTypeRefOfClassByShortName(processingEnv, annotatedClasses, name);

            if (typeMirror != null) {
                generateContract(processingEnv, annotatedElement, typeMirror, ContractRole.CONSUMER, topics.toArray(new String[0]), appName, null);
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Cannot generate contract. No inner TypeRef class found for generated class " + name);
            }
        }
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
