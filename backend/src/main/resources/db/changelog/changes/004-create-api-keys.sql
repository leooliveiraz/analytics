--liquibase formatted sql

--changeset analytics:004-create-api-keys
CREATE TABLE api_keys (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    name         VARCHAR(120) NOT NULL,
    key_prefix   VARCHAR(16)  NOT NULL,
    key_hash     VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_project ON api_keys (project_id);
CREATE INDEX idx_api_keys_prefix ON api_keys (key_prefix);
--rollback DROP TABLE api_keys;
