CREATE SEQUENCE sequence_instance_sequence START WITH 1 INCREMENT 1 CYCLE; -- sequence is not used via Hibernate, not caching 50 values
CREATE SEQUENCE buffered_message_sequence START WITH 1 INCREMENT 50 CYCLE;
CREATE SEQUENCE sequenced_message_sequence START WITH 1 INCREMENT 50 CYCLE;

CREATE TABLE sequence_instance
(
    id           bigint                   not null
        constraint sequence_instance_pkey primary key,
    name         text                     not null,
    context_id   text                     not null,
    state        text                     not null,
    created_at   timestamp with time zone NOT NULL,
    closed_at    timestamp with time zone,
    retain_until timestamp with time zone NOT NULL
);

ALTER TABLE sequence_instance
    ADD CONSTRAINT SEQUENCE_INSTANCE_NAME_CONTEXT_ID_UK UNIQUE (name, context_id);

CREATE TABLE sequenced_message
(
    id                   bigint                   not null
        constraint sequenced_message_pkey primary key,
    message_type         text                     not null,
    cluster_name         text                     not null,
    topic                text                     not null,
    sequenced_message_id UUID                     not null,
    idempotence_id       text                     not null,
    state                text                     not null,
    trace_id_high        bigint,
    trace_id             bigint,
    span_id              bigint,
    parent_span_id       bigint,
    trace_id_string      text,
    created_at           timestamp with time zone NOT NULL,
    state_changed_at     timestamp with time zone,
    sequence_instance_id bigint references sequence_instance
);

CREATE INDEX sequenced_message_sequence_instance_id ON sequenced_message (sequence_instance_id);
CREATE INDEX sequenced_message_idempotence_id ON sequenced_message (idempotence_id);
CREATE INDEX idx_sequenced_message_state_message_type ON sequenced_message (state, message_type); -- for metrics

CREATE TABLE buffered_message
(
    id                   bigint not null
        constraint buffered_message_pkey primary key,
    sequence_instance_id bigint references sequence_instance,
    sequenced_message_id bigint references sequenced_message,
    message_key          bytea,
    message_value        bytea  not null
);

CREATE INDEX buffered_message_sequence_instance_id ON buffered_message (sequence_instance_id);
CREATE INDEX buffered_message_sequenced_message_id ON buffered_message (sequenced_message_id);

CREATE SEQUENCE message_header_sequence START WITH 1 INCREMENT 50 CYCLE;

CREATE TABLE message_header
(
    id                  bigint not null
        constraint message_header_pkey primary key,
    buffered_message_id bigint not null references buffered_message,
    header_name         text   not null,
    header_value        bytea  not null
);

CREATE INDEX message_header_buffered_message_id ON message_header (buffered_message_id);

CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
