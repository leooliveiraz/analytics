--liquibase formatted sql

--changeset analytics:012-detailed-event-indexes
CREATE INDEX IF NOT EXISTS idx_events_project_path_name_time ON events (project_id, path, event_name, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_project_image ON events (project_id, image_key) WHERE image_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_project_element ON events (project_id, element_selector) WHERE element_selector IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_project_section ON events (project_id, section_key) WHERE section_key IS NOT NULL;
--rollback DROP INDEX IF EXISTS idx_events_project_section;
--rollback DROP INDEX IF EXISTS idx_events_project_element;
--rollback DROP INDEX IF EXISTS idx_events_project_image;
--rollback DROP INDEX IF EXISTS idx_events_project_path_name_time;
