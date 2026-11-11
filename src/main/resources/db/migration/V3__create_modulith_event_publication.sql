-- Spring Modulith JPA event publication registry table (validated by Hibernate at startup).
CREATE TABLE IF NOT EXISTS event_publication
(
    id                    UUID         NOT NULL PRIMARY KEY,
    completion_attempts   INTEGER      NOT NULL,
    completion_date       TIMESTAMPTZ,
    event_type            VARCHAR(255) NOT NULL,
    last_resubmission_date TIMESTAMPTZ,
    listener_id           VARCHAR(255) NOT NULL,
    publication_date      TIMESTAMPTZ  NOT NULL,
    serialized_event      VARCHAR(255) NOT NULL,
    status                VARCHAR(255),
    CONSTRAINT ck_event_publication_status CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

CREATE INDEX IF NOT EXISTS ix_event_publication_completion_date ON event_publication (completion_date);
