package ch.admin.bit.jeap.messaging.sequentialinbox.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Embeddable
public class SequentialInboxTraceContext {

    @Column(name = "trace_id_high")
    private Long traceIdHigh;

    @Column(name = "trace_id")
    private Long traceId;

    @Column(name = "span_id")
    private Long spanId;

    @Column(name = "parent_span_id")
    private Long parentSpanId;

    @Column(name = "trace_id_string")
    private String traceIdString;

    public static SequentialInboxTraceContext empty() {
        return new SequentialInboxTraceContext();
    }
}
