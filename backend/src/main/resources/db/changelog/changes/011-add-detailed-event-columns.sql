--liquibase formatted sql

--changeset analytics:011-add-detailed-event-columns
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS element_tag      VARCHAR(32),
    ADD COLUMN IF NOT EXISTS element_selector TEXT,
    ADD COLUMN IF NOT EXISTS element_text     VARCHAR(512),
    ADD COLUMN IF NOT EXISTS element_id       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS href             TEXT,
    ADD COLUMN IF NOT EXISTS click_x          INTEGER,
    ADD COLUMN IF NOT EXISTS click_y          INTEGER,
    ADD COLUMN IF NOT EXISTS click_x_pct      NUMERIC(6, 3),
    ADD COLUMN IF NOT EXISTS click_y_pct      NUMERIC(6, 3),
    ADD COLUMN IF NOT EXISTS viewport_width   INTEGER,
    ADD COLUMN IF NOT EXISTS viewport_height  INTEGER,
    ADD COLUMN IF NOT EXISTS page_height      INTEGER,
    ADD COLUMN IF NOT EXISTS duration_ms      INTEGER,
    ADD COLUMN IF NOT EXISTS engaged_ms       INTEGER,
    ADD COLUMN IF NOT EXISTS scroll_pct       SMALLINT,
    ADD COLUMN IF NOT EXISTS image_key        TEXT,
    ADD COLUMN IF NOT EXISTS image_alt        VARCHAR(512),
    ADD COLUMN IF NOT EXISTS dwell_ms         INTEGER,
    ADD COLUMN IF NOT EXISTS section_key      VARCHAR(255);
--rollback ALTER TABLE events DROP COLUMN IF EXISTS section_key, DROP COLUMN IF EXISTS dwell_ms, DROP COLUMN IF EXISTS image_alt, DROP COLUMN IF EXISTS image_key, DROP COLUMN IF EXISTS scroll_pct, DROP COLUMN IF EXISTS engaged_ms, DROP COLUMN IF EXISTS duration_ms, DROP COLUMN IF EXISTS page_height, DROP COLUMN IF EXISTS viewport_height, DROP COLUMN IF EXISTS viewport_width, DROP COLUMN IF EXISTS click_y_pct, DROP COLUMN IF EXISTS click_x_pct, DROP COLUMN IF EXISTS click_y, DROP COLUMN IF EXISTS click_x, DROP COLUMN IF EXISTS href, DROP COLUMN IF EXISTS element_id, DROP COLUMN IF EXISTS element_text, DROP COLUMN IF EXISTS element_selector, DROP COLUMN IF EXISTS element_tag;
