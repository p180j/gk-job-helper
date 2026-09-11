ALTER TABLE recruitment_match_result ADD COLUMN uncertain_reason VARCHAR(64) DEFAULT NULL AFTER summary;
ALTER TABLE recruitment_position ADD COLUMN extraction_quality VARCHAR(32) NOT NULL DEFAULT 'PARTIAL' AFTER source_type;
ALTER TABLE recruitment_position MODIFY COLUMN source_attachment_id BIGINT DEFAULT NULL;
