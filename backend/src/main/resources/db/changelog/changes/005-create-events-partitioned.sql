--liquibase formatted sql

--changeset analytics:005-create-events-partitioned
CREATE TABLE events (
    id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    project_id      UUID          NOT NULL,
    event_name      VARCHAR(120)  NOT NULL DEFAULT 'pageview',
    url             TEXT,
    path            VARCHAR(2048),
    referrer        TEXT,
    referrer_domain VARCHAR(255),
    utm_source      VARCHAR(255),
    utm_medium      VARCHAR(255),
    utm_campaign    VARCHAR(255),
    utm_term        VARCHAR(255),
    utm_content     VARCHAR(255),
    browser         VARCHAR(64),
    os              VARCHAR(64),
    device_type     VARCHAR(32),
    country         VARCHAR(2),
    region          VARCHAR(120),
    city            VARCHAR(120),
    language        VARCHAR(16),
    screen_width    INTEGER,
    screen_height   INTEGER,
    visitor_id      VARCHAR(64),
    session_id      UUID,
    properties      JSONB         NOT NULL DEFAULT '{}'::jsonb,
    occurred_at     TIMESTAMPTZ   NOT NULL,
    received_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

CREATE TABLE events_default PARTITION OF events DEFAULT;
--rollback DROP TABLE events;
