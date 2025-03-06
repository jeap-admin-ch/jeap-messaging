package ch.admin.bit.jeap.messaging.kafka.signature.benchmark;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureService;
import ch.admin.bit.jeap.messaging.kafka.signature.common.CryptoProviderHelper;
import ch.admin.bit.jeap.messaging.model.Message;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class SignatureRoundtripBenchmark {

    @State(Scope.Benchmark)
    public static class SignatureBenchmarkState {
        static {
            CryptoProviderHelper.installCryptoProvider();
        }

        private final SignatureService signatureService = Benchmarks.createSignatureService();
        private final SignatureAuthenticityService authenticityService = Benchmarks.createAuthenticityService();
        private final Message message = Benchmarks.createJmeDeclarationCreatedEvent();
        private final byte[] serializedBytes = Benchmarks.serializeJmeDeclarationCreatedEvent();
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void benchmarkSignatureRoundtripForKeyAndValue(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();
        boolean isKey = true;
        boolean notKey = false;
        byte[] bytesToSign = state.serializedBytes;
        byte[] bytesToSignForKey = state.serializedBytes;

        // Sign and verify key and value signature
        state.signatureService.injectSignature(headers, bytesToSign, notKey);
        state.signatureService.injectSignature(headers, bytesToSignForKey, isKey);
        Benchmarks.assertKeyAndValueSigned(headers);
        state.authenticityService.checkAuthenticityValue(state.message, headers, bytesToSign);
        state.authenticityService.checkAuthenticityKey(headers, bytesToSignForKey);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.signatureService);
        blackhole.consume(state.authenticityService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void benchmarkSignatureRoundtripForValue(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();
        boolean notKey = false;
        byte[] bytesToSign = state.serializedBytes;

        // Sign and verify value signature
        state.signatureService.injectSignature(headers, bytesToSign, notKey);
        Benchmarks.assertValueSigned(headers);
        state.authenticityService.checkAuthenticityValue(state.message, headers, bytesToSign);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.signatureService);
        blackhole.consume(state.authenticityService);
    }
}
