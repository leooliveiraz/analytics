--liquibase formatted sql

--changeset analytics:002-create-projects
CREATE TABLE projects (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    domain     VARCHAR(255),
    timezone   VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    public_key VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_projects_public_key UNIQUE (public_key)
);
--rollback DROP TABLE projects;
