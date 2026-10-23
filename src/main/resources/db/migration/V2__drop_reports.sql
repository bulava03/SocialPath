-- The reporting feature was removed from the application: nothing writes
-- or reads reports anymore.
ALTER TABLE admins DROP COLUMN report_id;

DROP TABLE reports;
