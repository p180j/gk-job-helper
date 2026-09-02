ALTER TABLE recruitment_notice ADD COLUMN detail_status VARCHAR(16) NOT NULL DEFAULT 'DISCOVERED' AFTER user_status;
ALTER TABLE recruitment_notice ADD COLUMN body_html LONGTEXT DEFAULT NULL AFTER detail_status;
ALTER TABLE recruitment_notice ADD COLUMN body_text LONGTEXT DEFAULT NULL AFTER body_html;
ALTER TABLE recruitment_notice ADD COLUMN detail_fetched_at DATETIME DEFAULT NULL AFTER body_text;
ALTER TABLE recruitment_notice ADD COLUMN detail_error VARCHAR(500) DEFAULT NULL AFTER detail_fetched_at;
CREATE TABLE recruitment_attachment (id BIGINT NOT NULL AUTO_INCREMENT,notice_id BIGINT NOT NULL,file_name VARCHAR(1000) NOT NULL,file_url VARCHAR(700) NOT NULL,file_type VARCHAR(16) NOT NULL,attachment_type VARCHAR(32) NOT NULL,source_text VARCHAR(1000) DEFAULT NULL,created_at DATETIME DEFAULT NULL,updated_at DATETIME DEFAULT NULL,PRIMARY KEY(id),UNIQUE KEY uk_recruitment_attachment_notice_url(notice_id,file_url),CONSTRAINT fk_recruitment_attachment_notice FOREIGN KEY(notice_id) REFERENCES recruitment_notice(id));
