package ch.admin.bit.jeap.messaging.sequentialinbox.configuration.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ReleaseCondition {

    private String predecessor;

    private final List<ReleaseCondition> and = new ArrayList<>();

    private final List<ReleaseCondition> or = new ArrayList<>();

    @Override
    public String toString() {
        return """
                ReleaseCondition{
                    predecessor='%s',
                    and=%s,
                    or=%s}
                """.formatted(predecessor, and, or);
    }
}
