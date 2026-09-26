--liquibase formatted sql

--changeset analytics:007-create-sessions
CREATE TABLE sessions (
    id               UUID          PRIMARY KEY,
    project_id       UUID          NOT NULL,
    visitor_id       VARCHAR(64)   NOT NULL,
    started_at       TIMESTAMPTZ   NOT NULL,
    ended_at         TIMESTAMPTZ,
    duration_seconds INTEGER       NOT NULL DEFAULT 0,
    pageviews        INTEGER       NOT NULL DEFAULT 0,
    entry_path       VARCHAR(2048),
    exit_path        VARCHAR(2048),
    is_bounce        BOOLEAN       NOT NULL DEFAULT TRUE,
    referrer_domain  VARCHAR(255),
    utm_source       VARCHAR(255),
    utm_medium       VARCHAR(255),
    utm_campaign     VARCHAR(255),
    country          VARCHAR(2),
    browser          VARCHAR(64),
    os               VARCHAR(64),
    device_type      VARCHAR(32),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_sessions_project_started ON sessions (project_id, started_at DESC);
CREATE INDEX idx_sessions_project_visitor ON sessions (project_id, visitor_id);
--rollback DROP TABLE sessions;
