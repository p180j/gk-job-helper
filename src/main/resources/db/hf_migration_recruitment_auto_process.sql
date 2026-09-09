ALTER TABLE recruitment_notice ADD COLUMN process_status VARCHAR(16) NOT NULL DEFAULT 'DISCOVERED' AFTER detail_status;
ALTER TABLE recruitment_notice ADD COLUMN process_stage VARCHAR(32) DEFAULT NULL AFTER process_status;
ALTER TABLE recruitment_notice ADD COLUMN process_failure_code VARCHAR(64) DEFAULT NULL AFTER process_stage;
ALTER TABLE recruitment_notice ADD COLUMN process_failure_reason VARCHAR(500) DEFAULT NULL AFTER process_failure_code;
ALTER TABLE recruitment_notice ADD COLUMN process_position_count INT NOT NULL DEFAULT 0 AFTER process_failure_reason;
ALTER TABLE recruitment_notice ADD COLUMN processed_at DATETIME DEFAULT NULL AFTER process_position_count;
