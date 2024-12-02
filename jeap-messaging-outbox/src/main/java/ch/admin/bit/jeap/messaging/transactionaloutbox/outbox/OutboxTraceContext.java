package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import lombok.*;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Builder(toBuilder = true)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED) // for JPA
@ToString
@Getter
@Embeddable
public class OutboxTraceContext {

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

}
