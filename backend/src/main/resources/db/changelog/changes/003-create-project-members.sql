--liquibase formatted sql

--changeset analytics:003-create-project-members
CREATE TABLE project_members (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_members UNIQUE (project_id, user_id),
    CONSTRAINT ck_project_members_role CHECK (role IN ('OWNER', 'ADMIN', 'VIEWER'))
);

CREATE INDEX idx_project_members_user ON project_members (user_id);
--rollback DROP TABLE project_members;
