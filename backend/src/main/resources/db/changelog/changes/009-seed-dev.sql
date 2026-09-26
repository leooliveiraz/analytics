--liquibase formatted sql

--changeset analytics:009-seed-demo-project context:dev
INSERT INTO projects (id, name, domain, timezone, public_key)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Demo Project',
    'localhost',
    'America/Sao_Paulo',
    'demo_public_key_0000000000000000000000000000'
);
--rollback DELETE FROM projects WHERE id = '00000000-0000-0000-0000-000000000001';
