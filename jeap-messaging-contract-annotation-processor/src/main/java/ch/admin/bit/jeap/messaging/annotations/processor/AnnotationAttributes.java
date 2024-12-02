package ch.admin.bit.jeap.messaging.annotations.processor;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContracts;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContracts;

import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
class AnnotationAttributes {
    static TypeMirror getTypeMirrorForValueAttribute(JeapMessageConsumerContract jeapMessageConsumerContract) {
        try {
            jeapMessageConsumerContract.value();
            throw new IllegalStateException("Expected MirroredTypeException");
        } catch (MirroredTypeException mirroredTypeException) {
            return mirroredTypeException.getTypeMirror();
        }
    }

    static TypeMirror getTypeMirrorForValueAttribute(JeapMessageProducerContract jeapMessageProducerContract) {
        try {
            jeapMessageProducerContract.value();
            throw new IllegalStateException("Expected MirroredTypeException");
        } catch (MirroredTypeException mirroredTypeException) {
            return mirroredTypeException.getTypeMirror();
        }
    }

    static List<TypeMirror> getTypeMirrorsForValueAttribute(JeapMessageConsumerContracts jeapMessageConsumerContracts) {
        try {
            jeapMessageConsumerContracts.value();
            throw new IllegalStateException("Expected MirroredTypesException");
        } catch (MirroredTypesException mirroredTypesException) {
            return new ArrayList<>(mirroredTypesException.getTypeMirrors());
        }
    }

    static List<TypeMirror> getTypeMirrorsForValueAttribute(JeapMessageProducerContracts jeapMessageProducerContracts) {
        try {
            jeapMessageProducerContracts.value();
            throw new IllegalStateException("Expected MirroredTypesException");
        } catch (MirroredTypesException mirroredTypesException) {
            return new ArrayList<>(mirroredTypesException.getTypeMirrors());
        }
    }
}
