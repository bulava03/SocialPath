-- A group whose topic is OTHER names it in its own words; only such a group
-- has one.
ALTER TABLE social_groups
    ADD COLUMN custom_category VARCHAR(30) NULL,
    ADD CONSTRAINT chk_groups_custom_category CHECK (custom_category IS NULL OR category = 'OTHER');
