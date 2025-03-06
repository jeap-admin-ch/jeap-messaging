# Kafka Message Signature Benchmarks

This directory contains JMH (Java Microbenchmark Harness) benchmarks for measuring the performance of Kafka message
signature generation and validation.

## Available Benchmarks

### 1. SignatureServiceBenchmark

Measures the performance of signature generation and injection into Kafka message headers:

- `benchmarkSignatureInjectionForValue`: Measures the time taken to generate and inject a signature for a message value
- `benchmarkSignatureInjectionForKeyAndValue`: Measures the time taken to generate and inject signatures for both
  message key and value

### 2. SignatureAuthenticityBenchmark

Measures the performance of signature validation:

- `validateValueSignature`: Measures the time taken to validate a signature for a message value
- `validateKeyAndValueSignatures`: Measures the time taken to validate signatures for both message key and value

### 2. SignatureRoundtripBenchmark

Measures the performance of signature generation and validation in a roundtrip scenario:

- `benchmarkSignatureRoundtripForValue`: Measures the time taken to generate and validate a signature for a message value
- `benchmarkSignatureRoundtripForKeyAndValue`: Measures the time taken to generate and validate signatures for both
  message key and value

## Running the Benchmarks

There are several ways to run the benchmarks:

### Method 1: Run in IDE

The simplest way is to run the `main` method in `SignatureBenchmarkRunner.java` directly from your IDE.
This will execute all benchmarks with default parameters.

### Method 2: Maven Command Line

Run the benchmarks using Maven with the test runner:

```bash
../mvnw test -Dtest=SignatureBenchmarkRunner
```

### Method 3: Custom JMH Parameters

To customize benchmark parameters (such as iterations, warmup, etc.), you can pass JMH options as program arguments:

```bash
../mvnw test -Dtest=SignatureBenchmarkRunner -Djmh.args="-i 3 -wi 1 -f 1"
```

Where:

- `-i 3`: Run 3 iterations of the benchmark
- `-wi 1`: Use 1 warmup iteration
- `-f 1`: Fork 1 JVM for the benchmark

To run a specific benchmark only:

```bash
../mvnw test -Dtest=SignatureBenchmarkRunner -Djmh.args="-i 3 -wi 1 -f 1 SignatureServiceBenchmark.benchmarkSignatureInjectionForValue"
```

## Benchmark Results

After running, JMH will display detailed performance results including:

- Average execution time (and standard deviation)
- Throughput (operations per second)
- Various percentile measurements

Example output:

```
Result "ch.admin.bit.jeap.messaging.kafka.signature.benchmark.SignatureServiceBenchmark.benchmarkSignatureInjectionForKeyAndValue":
  811.761 ±(99.9%) 1.525 ops/s [Average]
  (min, avg, max) = (811.288, 811.761, 812.241), stdev = 0.396
  CI (99.9%): [810.237, 813.286] (assumes normal distribution)
```

## Implementation Details

- The benchmarks use sample messages and certificates created in the `Benchmarks` utility class
- Test certificates and keys are loaded from the `/perftest` resources directory
- JMH settings use minimal forking and warmup iterations to run faster in development. Increase these values for more
  accurate results.
- Experiments have shown that differing payloads or keys used for signing or validation do not significantly affect
  the results. The benchmarks have thus been simplified to use a static payload for all tests.
