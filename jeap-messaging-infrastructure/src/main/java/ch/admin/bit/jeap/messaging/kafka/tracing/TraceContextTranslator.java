package ch.admin.bit.jeap.messaging.kafka.tracing;

import io.micrometer.tracing.Tracer;

/**
 * Converts between the jEAP {@link TraceContext} (Long-based, persisted to downstream libraries' databases) and the
 * Micrometer {@link io.micrometer.tracing.TraceContext} (hex-string-based, OTel-native).
 * <p>
 * OTel trace IDs are 32 hex chars (128 bit). The jEAP POJO stores them split across {@code traceIdHigh} (upper 64 bit)
 * and {@code traceId} (lower 64 bit) as unsigned Longs. Span IDs are 64 bit hex, stored as one Long. A zero parent id
 * from OTel is treated as "no parent" and surfaces as {@code null} on the POJO.
 */
final class TraceContextTranslator {

    private static final String ZERO_SPAN_ID = "0000000000000000";

    private TraceContextTranslator() {
    }

    static TraceContext toJeapTraceContext(io.micrometer.tracing.TraceContext source) {
        String traceIdHex = leftPad32(source.traceId());
        long traceIdHigh = Long.parseUnsignedLong(traceIdHex.substring(0, 16), 16);
        long traceIdLow = Long.parseUnsignedLong(traceIdHex.substring(16, 32), 16);
        long spanId = Long.parseUnsignedLong(source.spanId(), 16);
        String parentIdHex = source.parentId();
        Long parentSpanId = (parentIdHex == null || parentIdHex.isEmpty() || ZERO_SPAN_ID.equals(parentIdHex))
                ? null
                : Long.parseUnsignedLong(parentIdHex, 16);
        return new TraceContext(traceIdHigh, traceIdLow, spanId, parentSpanId, traceIdHex, source.sampled());
    }

    static io.micrometer.tracing.TraceContext toMicrometerTraceContext(Tracer tracer, TraceContext source) {
        String traceIdHex = (source.getTraceIdString() != null && source.getTraceIdString().length() == 32)
                ? source.getTraceIdString()
                : toHex32(nullToZero(source.getTraceIdHigh()), nullToZero(source.getTraceId()));
        // A null sampled flag represents a context persisted by a pre-OTel-migration jEAP version that did not record
        // the decision. Defaulting to TRUE in that case keeps behavior identical to what those older versions did
        // on replay. Contexts persisted by the current code carry the real decision.
        Boolean sampled = source.getSampled() != null ? source.getSampled() : Boolean.TRUE;
        io.micrometer.tracing.TraceContext.Builder builder = tracer.traceContextBuilder()
                .traceId(traceIdHex)
                .spanId(toHex16(nullToZero(source.getSpanId())))
                .sampled(sampled);
        if (source.getParentSpanId() != null) {
            builder.parentId(toHex16(source.getParentSpanId()));
        }
        return builder.build();
    }

    private static String leftPad32(String hex) {
        return hex.length() >= 32 ? hex : "0".repeat(32 - hex.length()) + hex;
    }

    private static String toHex32(long high, long low) {
        return String.format("%016x%016x", high, low);
    }

    private static String toHex16(long value) {
        return String.format("%016x", value);
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
