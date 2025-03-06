package ch.admin.bit.jeap.messaging.kafka.signature.benchmark;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class SignatureServiceBenchmark {

    @State(Scope.Benchmark)
    public static class SignatureBenchmarkState {
        static {
            CryptoProviderHelper.installCryptoProvider();
        }

        private final SignatureService signatureService = Benchmarks.createSignatureService();
        private final byte[] serializedBytes = Benchmarks.serializeJmeDeclarationCreatedEvent();
        private final byte[] serializedBytesLarge = Benchmarks.simulateLargeEvent();

        int i = 0;
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void benchmarkSignatureInjectionForValue(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();

        state.signatureService.injectSignature(headers, state.serializedBytes, false);

        Benchmarks.assertValueSigned(headers);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.signatureService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void benchmarkSignatureInjectionForValueLarge(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();

        state.signatureService.injectSignature(headers, state.serializedBytesLarge, false);

        Benchmarks.assertValueSigned(headers);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.signatureService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void benchmarkSignatureInjectionForKeyAndValue(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();

        state.signatureService.injectSignature(headers, state.serializedBytes, false);
        state.signatureService.injectSignature(headers, state.serializedBytes, true);

        Benchmarks.assertKeyAndValueSigned(headers);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.signatureService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void benchmarkSignatureInjectionForKeyAndValueLarge(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();

        state.signatureService.injectSignature(headers, state.serializedBytesLarge, false);
        state.signatureService.injectSignature(headers, state.serializedBytesLarge, true);

        Benchmarks.assertKeyAndValueSigned(headers);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.signatureService);
    }
}