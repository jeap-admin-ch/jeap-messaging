CREATE SEQUENCE sequence_instance_sequence START WITH 1 INCREMENT 1;
CREATE SEQUENCE buffered_message_sequence START WITH 1 INCREMENT 1;
CREATE SEQUENCE sequenced_message_sequence START WITH 1 INCREMENT 1;

CREATE TABLE sequence_instance
(
    id         bigint not null
        constraint sequence_instance_pkey primary key,
    type       text not null,
    context_id text not null,
    state      text not null
);

ALTER TABLE sequence_instance ADD CONSTRAINT SEQUENCE_INSTANCE_TYPE_CONTEXT_ID_UK UNIQUE (type, context_id);

CREATE TABLE buffered_message
(
    id            bigint not null
        constraint buffered_message_pkey primary key,
    message_key   bytea,
    message_value bytea not null,
    headers       text
);

CREATE TABLE sequenced_message
(
    id                   bigint                   not null
        constraint sequenced_message_pkey primary key,
    type                 text not null,
    sequenced_message_id UUID not null,
    state                text not null,
    max_delay_period     text not null,
    trace_id_high        bigint,
    trace_id             bigint,
    span_id              bigint,
    parent_span_id       bigint,
    trace_id_string      text,
    created              timestamp with time zone NOT NULL,
    sequence_instance_id bigint references sequence_instance,
    message_id           bigint references buffered_message
);

create index sequenced_message_sequence_instance_id ON sequenced_message (sequence_instance_id);
create index sequenced_message_message_id ON sequenced_message (message_id);



