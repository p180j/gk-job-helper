ALTER TABLE recruitment_position MODIFY COLUMN source_attachment_id BIGINT DEFAULT NULL;
ALTER TABLE recruitment_position ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'ATTACHMENT_EXCEL' AFTER source_attachment_id;
