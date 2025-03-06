package ch.admin.bit.jeap.messaging.kafka.signature.benchmark;

import ch.admin.bit.jeap.messaging.kafka.signature.SignatureAuthenticityService;
import ch.admin.bit.jeap.messaging.kafka.signature.SignatureHeaders;
import ch.admin.bit.jeap.messaging.model.Message;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HexFormat;

public class SignatureAuthenticityBenchmark {

    private static final byte[] CERT_SERIAL_NR = HexFormat.of().parseHex("5c7cf317f5e4cf00120850b60a94c0594dc964e3");

    @State(Scope.Benchmark)
    public static class SignatureBenchmarkState {
        private final SignatureAuthenticityService authenticityService = Benchmarks.createAuthenticityService();

        private final byte[] serializedBytes = Benchmarks.serializeJmeDeclarationCreatedEvent();
        private final byte[] signatureBytes = Benchmarks.sign(serializedBytes);
        private final byte[] serializedBytesLarge = Benchmarks.simulateLargeEvent();
        private final byte[] signatureBytesLarge = Benchmarks.sign(serializedBytesLarge);

        private final Message message = Benchmarks.createJmeDeclarationCreatedEvent();
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void validateValueSignature(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();
        headers.add(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, CERT_SERIAL_NR);
        headers.add(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY, state.signatureBytes);

        // Benchmark the overhead added by validating the value signature
        state.authenticityService.checkAuthenticityValue(state.message, headers, state.serializedBytes);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.authenticityService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void validateKeyAndValueSignatures(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();
        headers.add(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, CERT_SERIAL_NR);
        headers.add(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY, state.signatureBytes);
        headers.add(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY, state.signatureBytes);

        // Benchmark the overhead added by validating the key and value signatures
        state.authenticityService.checkAuthenticityKey(headers, state.serializedBytes);
        state.authenticityService.checkAuthenticityValue(state.message, headers, state.serializedBytes);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.authenticityService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void validateValueSignatureLarge(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();
        headers.add(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, CERT_SERIAL_NR);
        headers.add(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY, state.signatureBytesLarge);

        // Benchmark the overhead added by validating the value signature
        state.authenticityService.checkAuthenticityValue(state.message, headers, state.serializedBytesLarge);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.authenticityService);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    public void validateKeyAndValueSignaturesLarge(SignatureBenchmarkState state, Blackhole blackhole) {
        Headers headers = new RecordHeaders();
        headers.add(SignatureHeaders.SIGNATURE_CERTIFICATE_HEADER_KEY, CERT_SERIAL_NR);
        headers.add(SignatureHeaders.SIGNATURE_VALUE_HEADER_KEY, state.signatureBytesLarge);
        headers.add(SignatureHeaders.SIGNATURE_KEY_HEADER_KEY, state.signatureBytesLarge);

        // Benchmark the overhead added by validating the key and value signatures
        state.authenticityService.checkAuthenticityKey(headers, state.serializedBytesLarge);
        state.authenticityService.checkAuthenticityValue(state.message, headers, state.serializedBytesLarge);

        // Prevent the JVM from optimizing away the benchmark
        blackhole.consume(state.authenticityService);
    }
}
