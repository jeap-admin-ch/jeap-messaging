package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
class ReleaseCondition {

    private String predecessor;

    private final List<ReleaseCondition> and = new ArrayList<>();

    private final List<ReleaseCondition> or = new ArrayList<>();

    @Override
    public String toString() {
        return "ReleaseCondition{predecessor='%s',and=%s,or=%s}".formatted(predecessor, and, or);
    }

    boolean isSatisfied(Set<String> releasedMessageTypes) {
        if (predecessor != null) {
            return releasedMessageTypes.contains(predecessor);
        }

        if (!and.isEmpty()) {
            return and.stream().allMatch(rc -> rc.isSatisfied(releasedMessageTypes));
        }

        return or.stream().anyMatch(rc -> rc.isSatisfied(releasedMessageTypes));
    }
}
