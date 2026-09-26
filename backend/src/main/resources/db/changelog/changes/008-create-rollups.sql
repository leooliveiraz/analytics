--liquibase formatted sql

--changeset analytics:008-create-rollups
CREATE TABLE project_daily_stats (
    project_id             UUID        NOT NULL,
    date                   DATE        NOT NULL,
    visitors               BIGINT      NOT NULL DEFAULT 0,
    pageviews              BIGINT      NOT NULL DEFAULT 0,
    sessions               BIGINT      NOT NULL DEFAULT 0,
    bounces                BIGINT      NOT NULL DEFAULT 0,
    total_duration_seconds BIGINT      NOT NULL DEFAULT 0,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, date)
);

CREATE TABLE dimension_daily_stats (
    project_id UUID         NOT NULL,
    date       DATE         NOT NULL,
    dimension  VARCHAR(40)  NOT NULL,
    value      VARCHAR(255) NOT NULL,
    visitors   BIGINT       NOT NULL DEFAULT 0,
    pageviews  BIGINT       NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, date, dimension, value)
);

CREATE INDEX idx_dimension_daily_stats_lookup ON dimension_daily_stats (project_id, dimension, date DESC);
--rollback DROP TABLE dimension_daily_stats;
--rollback DROP TABLE project_daily_stats;
