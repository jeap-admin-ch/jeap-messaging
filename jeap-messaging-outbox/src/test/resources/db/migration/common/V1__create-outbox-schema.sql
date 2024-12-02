CREATE SEQUENCE deferred_message_sequence START WITH 1 INCREMENT 1;

CREATE TABLE deferred_message
(
    id                     bigint PRIMARY KEY,
    message                bytea                    NOT NULL,
    "key"                  bytea,
    cluster_name           varchar,
    topic                  varchar                  NOT NULL,
    message_id             varchar                  NOT NULL,
    message_idempotence_id varchar                  NOT NULL,
    message_type_name      varchar                  NOT NULL,
    message_type_version   varchar,
    created                timestamp with time zone NOT NULL,
    send_immediately       boolean,
    schedule_after         timestamp with time zone,
    sent_immediately       timestamp with time zone,
    sent_scheduled         timestamp with time zone,
    failed                 timestamp with time zone,
    fail_reason            varchar,
    resend                 boolean DEFAULT FALSE,
    trace_id_high          bigint,
    trace_id               bigint,
    span_id                bigint,
    parent_span_id         bigint,
    trace_id_string        varchar
);

CREATE INDEX deferred_message_created ON deferred_message (created);
CREATE INDEX deferred_message_send_immediately ON deferred_message (send_immediately);
CREATE INDEX deferred_message_schedule_after ON deferred_message (schedule_after);
CREATE INDEX deferred_message_sent_immediately ON deferred_message (sent_immediately);
CREATE INDEX deferred_message_sent_scheduled ON deferred_message (sent_scheduled);
CREATE INDEX deferred_message_failed ON deferred_message (failed);
CREATE INDEX deferred_message_resend ON deferred_message (resend);

CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
