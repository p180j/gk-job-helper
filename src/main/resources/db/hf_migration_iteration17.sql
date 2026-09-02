ALTER TABLE recruitment_attachment ADD INDEX idx_recruitment_attachment_notice (notice_id);
ALTER TABLE recruitment_attachment DROP INDEX uk_recruitment_attachment_notice_url;
ALTER TABLE recruitment_attachment ADD COLUMN dedupe_key CHAR(64) NOT NULL AFTER file_url;
UPDATE recruitment_attachment SET dedupe_key = SHA2(CONCAT(file_url, '\n', file_name), 256) WHERE dedupe_key = '';
ALTER TABLE recruitment_attachment ADD UNIQUE KEY uk_recruitment_attachment_notice_dedupe (notice_id, dedupe_key);
