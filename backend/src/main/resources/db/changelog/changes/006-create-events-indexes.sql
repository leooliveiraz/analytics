--liquibase formatted sql

--changeset analytics:006-create-events-indexes
CREATE INDEX idx_events_project_time ON events (project_id, occurred_at DESC);
CREATE INDEX idx_events_project_name_time ON events (project_id, event_name, occurred_at DESC);
CREATE INDEX idx_events_project_path ON events (project_id, path);
CREATE INDEX idx_events_project_visitor ON events (project_id, visitor_id, occurred_at);
CREATE INDEX idx_events_project_session ON events (project_id, session_id);
CREATE INDEX idx_events_project_referrer ON events (project_id, referrer_domain);
CREATE INDEX idx_events_project_country ON events (project_id, country);
CREATE INDEX idx_events_project_device ON events (project_id, device_type);
--rollback DROP INDEX idx_events_project_device;
--rollback DROP INDEX idx_events_project_country;
--rollback DROP INDEX idx_events_project_referrer;
--rollback DROP INDEX idx_events_project_session;
--rollback DROP INDEX idx_events_project_visitor;
--rollback DROP INDEX idx_events_project_path;
--rollback DROP INDEX idx_events_project_name_time;
--rollback DROP INDEX idx_events_project_time;
