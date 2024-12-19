package ch.admin.bit.jeap.messaging.annotations.processor.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

public class TypeRefFinder {

    public static TypeMirror findTypeRefOfClassByShortName(ProcessingEnvironment processingEnv, Set<Class<?>> classes, String shortName) {
        Class<?> messageClass = findClassByShortName(classes, shortName);
        if (messageClass != null) {
            for (Class<?> innerClass : messageClass.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals("TypeRef")) {
                    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(innerClass.getCanonicalName());
                    return typeElement.asType();
                }
            }
        }
        return null;
    }

    static Class<?> findClassByShortName(Set<Class<?>> classes, String shortName) {
        return classes.stream()
                .filter(clazz -> clazz.getSimpleName().equals(shortName))
                .findFirst()
                .orElse(null);
    }
}
