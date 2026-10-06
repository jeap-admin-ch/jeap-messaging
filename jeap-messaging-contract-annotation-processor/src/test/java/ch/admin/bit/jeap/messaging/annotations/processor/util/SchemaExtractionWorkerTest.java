package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class SchemaExtractionWorkerTest {

    @Test
    void timeoutAbandonsUninterruptibleOperationAndDisablesFurtherExtraction() {
        var release = new CountDownLatch(1);
        var workerThread = new AtomicReference<Thread>();
        var worker = new SchemaExtractionWorker(Duration.ofSeconds(1), Duration.ofMillis(20));
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                var result = worker.execute(null, () -> {
                    workerThread.set(Thread.currentThread());
                    boolean released = false;
                    while (!released) {
                        try {
                            released = release.await(1, TimeUnit.SECONDS);
                        } catch (InterruptedException ignored) {
                            // Simulate application initialization that ignores interruption.
                        }
                    }
                    return new SchemaExtractionWorker.Result("late schema", null);
                });
                assertThat(result.schema()).isNull();
                assertThat(result.diagnostic()).contains("timed out");
                assertThat(workerThread.get()).isNotNull();
                assertThat(workerThread.get().isAlive()).isTrue();
                assertThat(workerThread.get().isDaemon()).isTrue();
                assertThat(worker.execute(null, () -> {
                    throw new AssertionError("Stopped workers must not run another extraction");
                }).diagnostic()).contains("worker stopped");
            });
        } finally {
            release.countDown();
            worker.run();
            Thread thread = workerThread.get();
            if (thread != null) {
                assertTimeoutPreemptively(Duration.ofSeconds(5), () -> { thread.join(); });
            }
        }
    }

    @Test
    void interruptionIsPreservedAndStopsExtraction() {
        var worker = new SchemaExtractionWorker();
        try {
            Thread.currentThread().interrupt();
            var result = worker.execute(null, () -> {
                new CountDownLatch(1).await();
                return new SchemaExtractionWorker.Result("unreachable", null);
            });
            assertThat(result.schema()).isNull();
            assertThat(result.diagnostic()).contains("Interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
            worker.run();
        }
    }

    @Test
    void closedLoaderReportsClassNotFound() {
        var loader = new CompilerClasspathLoader((packageName, fileName) -> {
            throw new AssertionError("Closed loader must not request compiler resources");
        });
        loader.close();
        assertThatThrownBy(() -> loader.loadClass("example.Message"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("loader closed");
    }
}
