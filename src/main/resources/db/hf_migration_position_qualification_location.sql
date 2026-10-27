-- Apply once before deploying the new backend. Existing source rows and match results are untouched.
ALTER TABLE job_position_feature
    ADD COLUMN effective_major_requirement TEXT NULL,
    ADD COLUMN remark_conditions TEXT NULL,
    ADD COLUMN work_location_name VARCHAR(255) NULL,
    ADD COLUMN work_location_code VARCHAR(32) NULL,
    ADD COLUMN work_location_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    ADD COLUMN work_location_source VARCHAR(32) NULL,
    ADD COLUMN work_location_evidence TEXT NULL,
    ADD COLUMN work_location_type VARCHAR(32) NULL;
CREATE INDEX idx_position_feature_work_location ON job_position_feature (work_location_status, work_location_code);
