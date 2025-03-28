package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplateMessageCollectorTest {

    private TemplateMessageCollector collector;
    private Messager messager;

    @BeforeEach
    void setUp() {
        messager = mock(Messager.class);
        collector = new TemplateMessageCollector();
    }

    @Test
    void testCollectTemplateMessages() {
        Path resourceDir = Paths.get("src/test/resources/process/templates");

        Map<String, Set<String>> result = collector.collectTemplateMessages(resourceDir.toString(), messager);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("JmeRacePreparedEvent"));
        assertTrue(result.containsKey("JmeRaceStartedEvent"));
        assertEquals(Set.of("jme-race-prepared"), result.get("JmeRacePreparedEvent"));
        assertEquals(Set.of("jme-race-started", "jme-race-started-tv"), result.get("JmeRaceStartedEvent"));
    }

    @Test
    void testCollectTemplateMessagesWithInvalidPath() {
        String invalidPath = "invalid/path";

        Map<String, Set<String>> result = collector.collectTemplateMessages(invalidPath, messager);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(messager).printMessage(eq(Diagnostic.Kind.ERROR), contains("Error reading directory: " + invalidPath));
    }
}
