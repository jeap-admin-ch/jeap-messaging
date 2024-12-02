package ch.admin.bit.jeap.messaging.registry.helper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticVersionTests {
    @Test
    void toStringTest() {
        SemanticVersion target = new SemanticVersion(1, 4, 28);
        assertEquals("1.4.28", target.toString());
    }

    @Test
    void ordering() {
        List<SemanticVersion> unordered = List.of(
                new SemanticVersion(1, 4, 0),
                new SemanticVersion(2, 0, 0),
                new SemanticVersion(1, 2, 1),
                new SemanticVersion(1, 0, 3),
                new SemanticVersion(1, 0, 0));

        List<SemanticVersion> ordered = unordered.stream().sorted().toList();

        assertThat(ordered)
                .containsExactly(
                        new SemanticVersion(1, 0, 0),
                        new SemanticVersion(1, 0, 3),
                        new SemanticVersion(1, 2, 1),
                        new SemanticVersion(1, 4, 0),
                        new SemanticVersion(2, 0, 0));
    }
}
