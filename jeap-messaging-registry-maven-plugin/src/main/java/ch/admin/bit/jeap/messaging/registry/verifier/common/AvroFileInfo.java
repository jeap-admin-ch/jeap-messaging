package ch.admin.bit.jeap.messaging.registry.verifier.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;

@RequiredArgsConstructor
@ToString
class AvroFileInfo {
    @Getter
    private final String importPath;
    private Set<AvroTypeInfo> definedTypes = new LinkedHashSet<>();

    void addDefinedType(AvroTypeInfo type) {
        definedTypes.add(type);
    }

    public boolean containsType(String referencedType) {
        return definedTypes.stream().anyMatch(t -> t.isSameType(referencedType));
    }
}
