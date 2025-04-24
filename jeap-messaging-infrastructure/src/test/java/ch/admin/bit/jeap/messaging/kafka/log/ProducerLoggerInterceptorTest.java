package ch.admin.bit.jeap.messaging.kafka.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class ProducerLoggerInterceptorTest {

    @Mock
    private ProducerRecord<Object, Object> record;

    private PrintStream originalSysOut;
    private ByteArrayOutputStream logCapture;

    @BeforeEach
    void setUp() {
        originalSysOut = System.out;
        logCapture = new ByteArrayOutputStream();
        PrintStream outCapture = new PrintStream(logCapture);
        System.setOut(outCapture);
    }

    @AfterEach
    void tearDown() throws IOException {
        System.setOut(originalSysOut);
        logCapture.close();
    }

    @Test
    void onSend_whenSendingMessage_shouldLogMessage() {
        ProducerLoggerInterceptor interceptor = new ProducerLoggerInterceptor();
        TestEvent message = new TestEvent();
        doReturn(message).when(record).value();
        doReturn("topic").when(record).topic();
        interceptor.configure(Map.of(
                ProducerLoggerInterceptor.CLUSTER_NAME_CONFIG, "test"));

        interceptor.onSend(record);

        originalSysOut.println(logCapture.toString());
        assertThat(logCapture.toString())
                .containsPattern("INFO.*" + Pattern.quote("Published TestEvent (id) to topic (0) using cluster test"));
    }

    @Test
    void onSend_whenSendingDebugLevelMessage_shouldLogOnDebugLevel() {
        ch.qos.logback.classic.Logger logger =
                (Logger) LoggerFactory.getLogger(ProducerLoggerInterceptor.class);
        logger.setLevel(Level.DEBUG);

        ProducerLoggerInterceptor interceptor = new ProducerLoggerInterceptor();
        doReturn(new ReactionIdentifiedEvent()).when(record).value();
        doReturn("topic").when(record).topic();
        interceptor.configure(Map.of(
                ProducerLoggerInterceptor.CLUSTER_NAME_CONFIG, "test"));

        interceptor.onSend(record);

        originalSysOut.println(logCapture.toString());
        assertThat(logCapture.toString())
                .containsPattern("DEBUG.*" + Pattern.quote("Published ReactionIdentifiedEvent to topic (0) using cluster test"));
    }

    @Test
    void onSend_whenSendingNonMessage_shouldLogType() {
        ProducerLoggerInterceptor interceptor = new ProducerLoggerInterceptor();
        doReturn("some string").when(record).value();
        doReturn("topic").when(record).topic();
        interceptor.configure(Map.of(
                ProducerLoggerInterceptor.CLUSTER_NAME_CONFIG, "test"));

        interceptor.onSend(record);

        originalSysOut.println(logCapture.toString());
        assertThat(logCapture.toString())
                .contains("INFO");
        assertThat(logCapture.toString())
                .containsPattern("INFO.*" + Pattern.quote("Published String to topic (0) using cluster test"));
    }

    static class ReactionIdentifiedEvent {
    }
}
