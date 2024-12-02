-- The size restrictions on the composite primary key fields of the idempotent_processing table
-- are due to requirements of the AWS Database Migration Service (DMS). For more information, see:
-- https://docs.aws.amazon.com/dms/latest/userguide/CHAP_Validating.html
-- Select sizes that suit your specific needs while ensuring compliance with DMS requirements
-- if you plan to use AWS DMS for database migration or validation.
CREATE TABLE  idempotent_processing
(
    idempotence_id         varchar(200)             NOT NULL,
    idempotence_id_context varchar(200)             NOT NULL,
    created_at             timestamp with time zone NOT NULL,
    CONSTRAINT pk_idempotent_processing PRIMARY KEY (idempotence_id, idempotence_id_context)
);

CREATE TABLE shedlock (
    name                VARCHAR(64)                 NOT NULL,
    lock_until          TIMESTAMP                   NOT NULL,
    locked_at           TIMESTAMP                   NOT NULL,
    locked_by           VARCHAR(255)                NOT NULL,
    PRIMARY KEY (name)
);
