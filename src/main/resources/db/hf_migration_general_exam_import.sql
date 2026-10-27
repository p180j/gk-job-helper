-- Apply once on existing MySQL databases after backup, before deploying this parser version.
ALTER TABLE import_file
    ADD COLUMN file_checksum CHAR(64) NULL,
    ADD COLUMN parser_version VARCHAR(32) NULL,
    ADD COLUMN processed_rows INT NOT NULL DEFAULT 0,
    ADD COLUMN success_rows INT NOT NULL DEFAULT 0,
    ADD COLUMN failed_rows INT NOT NULL DEFAULT 0,
    ADD COLUMN error_message VARCHAR(1000) NULL;

ALTER TABLE job_position
    ADD COLUMN department_code VARCHAR(64) NULL,
    ADD COLUMN additional_requirements TEXT NULL,
    ADD COLUMN work_location VARCHAR(500) NULL,
    ADD COLUMN settlement_location VARCHAR(500) NULL;

-- Check existing duplicates by (import_file_id, source_sheet, source_row) before applying this index.
CREATE UNIQUE INDEX uk_job_position_source ON job_position (import_file_id, source_sheet, source_row);
