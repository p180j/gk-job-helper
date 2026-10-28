-- =============================================================
-- DEPLOYMENT ENTRYPOINT: INCREMENTAL
-- 公考智能选岗助手存量数据库统一增量脚本
-- 适用：已经存在基础表结构的 MySQL 5.7+ / 8.0 数据库。
-- 用法：先备份数据库并 USE gk_job_helper，然后完整执行本文件。
-- 范围：合并原 Iteration 7 之后的全部历史增量；全新数据库只执行 schema.sql。
-- 特性：表、列和索引按当前结构补齐，可安全重复执行；不会删除业务数据。
-- =============================================================

-- 历史迭代新增的独立表。表已存在时不会重建或覆盖数据。
CREATE TABLE IF NOT EXISTS job_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT, profile_id BIGINT NOT NULL, position_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    CONSTRAINT fk_job_favorite_profile FOREIGN KEY (profile_id) REFERENCES user_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_favorite_position FOREIGN KEY (position_id) REFERENCES job_position (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS job_interview_score (
    id BIGINT NOT NULL AUTO_INCREMENT, exam_year INT NOT NULL, position_code VARCHAR(64) NOT NULL,
    department_name VARCHAR(255) DEFAULT NULL, position_name VARCHAR(255) DEFAULT NULL,
    min_interview_score DECIMAL(8,2) NOT NULL, interview_candidate_count INT NOT NULL,
    source_file_name VARCHAR(255) NOT NULL, created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL, PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS job_position_feature (
    position_id BIGINT NOT NULL, exam_subject_count INT DEFAULT NULL, exam_subjects TEXT,
    exam_subject_group VARCHAR(512) DEFAULT NULL, exam_subject_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    raw_exam_subject_text VARCHAR(1000) DEFAULT NULL, major_restriction_type VARCHAR(32) NOT NULL DEFAULT 'UNCERTAIN',
    major_domains TEXT, major_similarity_keys TEXT, major_scope_count INT DEFAULT NULL,
    major_analysis_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN', organization_level VARCHAR(32) DEFAULT NULL,
    organization_level_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN', effective_major_requirement TEXT,
    remark_conditions TEXT, work_location_name VARCHAR(255) DEFAULT NULL, work_location_code VARCHAR(32) DEFAULT NULL,
    work_location_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN', work_location_source VARCHAR(32) DEFAULT NULL,
    work_location_evidence TEXT, work_location_type VARCHAR(32) DEFAULT NULL,
    created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL, PRIMARY KEY (position_id),
    CONSTRAINT fk_job_position_feature_position FOREIGN KEY (position_id) REFERENCES job_position (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS job_preference (
    id BIGINT NOT NULL AUTO_INCREMENT, profile_id BIGINT NOT NULL, preferred_regions TEXT,
    accepted_org_levels TEXT, excluded_org_levels TEXT, preferred_subject_groups TEXT,
    accept_extra_subjects TINYINT NOT NULL DEFAULT 1, prefer_more_recruits TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    CONSTRAINT fk_job_preference_profile FOREIGN KEY (profile_id) REFERENCES user_profile (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS career_profile (
    id BIGINT NOT NULL AUTO_INCREMENT, profile_id BIGINT NOT NULL,
    current_position VARCHAR(255) DEFAULT NULL, total_work_years VARCHAR(64) DEFAULT NULL,
    career_directions TEXT DEFAULT NULL, industries TEXT DEFAULT NULL, education_experiences TEXT DEFAULT NULL,
    work_experiences TEXT DEFAULT NULL, project_experiences TEXT DEFAULT NULL, skills TEXT DEFAULT NULL,
    certificates TEXT DEFAULT NULL, created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_career_profile_profile (profile_id)
);

CREATE TABLE IF NOT EXISTS career_fact (
    id BIGINT NOT NULL AUTO_INCREMENT, profile_id BIGINT NOT NULL, fact_type VARCHAR(64) NOT NULL,
    fact_key VARCHAR(128) NOT NULL, value_type VARCHAR(16) NOT NULL, boolean_value TINYINT DEFAULT NULL,
    number_value DECIMAL(12,2) DEFAULT NULL, text_value VARCHAR(1000) DEFAULT NULL, unit VARCHAR(32) DEFAULT NULL,
    display_name VARCHAR(255) NOT NULL, source VARCHAR(32) NOT NULL DEFAULT 'USER_CONFIRMED',
    confirmed_at DATETIME DEFAULT NULL, created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_career_fact_profile_key (profile_id, fact_type, fact_key),
    KEY idx_career_fact_profile (profile_id)
);

CREATE TABLE IF NOT EXISTS resume_file (
    id BIGINT NOT NULL AUTO_INCREMENT, profile_id BIGINT NOT NULL, original_filename VARCHAR(512) NOT NULL,
    file_type VARCHAR(128) NOT NULL, file_size BIGINT NOT NULL, storage_path VARCHAR(1024) NOT NULL,
    uploaded_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_resume_file_profile (profile_id)
);

CREATE TABLE IF NOT EXISTS recruitment_source (
    id BIGINT NOT NULL AUTO_INCREMENT, source_code VARCHAR(64) NOT NULL, source_name VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL, list_url VARCHAR(1000) NOT NULL, enabled TINYINT NOT NULL DEFAULT 1,
    last_fetch_time DATETIME DEFAULT NULL, last_fetch_status VARCHAR(255) DEFAULT NULL,
    created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_recruitment_source_code (source_code)
);

CREATE TABLE IF NOT EXISTS recruitment_notice (
    id BIGINT NOT NULL AUTO_INCREMENT, source_id BIGINT NOT NULL, title VARCHAR(1000) NOT NULL,
    notice_url VARCHAR(512) NOT NULL, publish_date DATETIME DEFAULT NULL, notice_type VARCHAR(32) NOT NULL,
    parent_notice_id BIGINT DEFAULT NULL, user_status VARCHAR(32) NOT NULL,
    detail_status VARCHAR(16) NOT NULL DEFAULT 'DISCOVERED', process_status VARCHAR(16) NOT NULL DEFAULT 'DISCOVERED',
    process_stage VARCHAR(32) DEFAULT NULL, process_failure_code VARCHAR(64) DEFAULT NULL,
    process_failure_reason VARCHAR(500) DEFAULT NULL, process_position_count INT NOT NULL DEFAULT 0,
    processed_at DATETIME DEFAULT NULL, body_html LONGTEXT DEFAULT NULL, body_text LONGTEXT DEFAULT NULL,
    detail_fetched_at DATETIME DEFAULT NULL, detail_error VARCHAR(500) DEFAULT NULL,
    viewed_at DATETIME DEFAULT NULL, discovered_at DATETIME NOT NULL, created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_recruitment_notice_source_url (source_id, notice_url),
    KEY idx_recruitment_notice_status (user_status), KEY idx_recruitment_notice_process (process_status),
    CONSTRAINT fk_recruitment_notice_source FOREIGN KEY (source_id) REFERENCES recruitment_source (id)
);

CREATE TABLE IF NOT EXISTS recruitment_attachment (
    id BIGINT NOT NULL AUTO_INCREMENT, notice_id BIGINT NOT NULL, file_name VARCHAR(1000) NOT NULL,
    file_url VARCHAR(700) NOT NULL, dedupe_key CHAR(64) NULL, file_type VARCHAR(16) NOT NULL,
    attachment_type VARCHAR(32) NOT NULL, parse_status VARCHAR(16) NOT NULL DEFAULT 'UNPARSED',
    parse_error VARCHAR(500) DEFAULT NULL, parsed_at DATETIME DEFAULT NULL, position_count INT NOT NULL DEFAULT 0,
    source_text VARCHAR(1000) DEFAULT NULL, created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id), CONSTRAINT fk_recruitment_attachment_notice FOREIGN KEY (notice_id) REFERENCES recruitment_notice (id)
);

CREATE TABLE IF NOT EXISTS recruitment_position (
    id BIGINT NOT NULL AUTO_INCREMENT, notice_id BIGINT NOT NULL, source_attachment_id BIGINT DEFAULT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'ATTACHMENT_EXCEL', extraction_quality VARCHAR(32) NOT NULL DEFAULT 'PARTIAL',
    organization_name VARCHAR(255) DEFAULT NULL, department_name VARCHAR(255) DEFAULT NULL,
    position_name VARCHAR(255) NOT NULL, position_code VARCHAR(128) DEFAULT NULL, recruit_count INT DEFAULT NULL,
    work_location VARCHAR(255) DEFAULT NULL, education_requirement VARCHAR(500) DEFAULT NULL,
    degree_requirement VARCHAR(500) DEFAULT NULL, major_requirement TEXT, age_requirement VARCHAR(500) DEFAULT NULL,
    work_years_requirement VARCHAR(500) DEFAULT NULL, responsibility TEXT, other_requirement TEXT,
    preferred_requirement TEXT, raw_requirement TEXT, source_sheet VARCHAR(255) NOT NULL, source_row INT NOT NULL,
    created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    CONSTRAINT fk_recruitment_position_notice FOREIGN KEY (notice_id) REFERENCES recruitment_notice (id),
    CONSTRAINT fk_recruitment_position_attachment FOREIGN KEY (source_attachment_id) REFERENCES recruitment_attachment (id)
);

CREATE TABLE IF NOT EXISTS recruitment_requirement (
    id BIGINT NOT NULL AUTO_INCREMENT, position_id BIGINT NOT NULL, requirement_type VARCHAR(32) NOT NULL,
    requirement_level VARCHAR(16) NOT NULL, raw_text TEXT, normalized_value VARCHAR(1000) DEFAULT NULL,
    source_text TEXT, created_at DATETIME DEFAULT NULL, PRIMARY KEY (id),
    CONSTRAINT fk_recruitment_requirement_position FOREIGN KEY (position_id) REFERENCES recruitment_position (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS recruitment_match_result (
    id BIGINT NOT NULL AUTO_INCREMENT, profile_id BIGINT NOT NULL, position_id BIGINT NOT NULL,
    match_result VARCHAR(16) NOT NULL, matched_count INT NOT NULL DEFAULT 0, uncertain_count INT NOT NULL DEFAULT 0,
    not_matched_count INT NOT NULL DEFAULT 0, summary VARCHAR(1000) DEFAULT NULL,
    uncertain_reason VARCHAR(64) DEFAULT NULL, created_at DATETIME DEFAULT NULL, updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS recruitment_match_item (
    id BIGINT NOT NULL AUTO_INCREMENT, match_result_id BIGINT NOT NULL, requirement_id BIGINT DEFAULT NULL,
    requirement_type VARCHAR(32) NOT NULL, requirement_level VARCHAR(16) NOT NULL, result VARCHAR(16) NOT NULL,
    position_requirement TEXT, user_evidence TEXT, reason VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME DEFAULT NULL, PRIMARY KEY (id)
);

DELIMITER $$

DROP PROCEDURE IF EXISTS hf_add_column_if_missing$$
CREATE PROCEDURE hf_add_column_if_missing(IN p_table_name VARCHAR(64), IN p_column_name VARCHAR(64), IN p_column_definition VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table_name AND COLUMN_NAME = p_column_name) THEN
        SET @hf_sql = CONCAT('ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
                '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'), '` ', p_column_definition);
        PREPARE hf_statement FROM @hf_sql;
        EXECUTE hf_statement;
        DEALLOCATE PREPARE hf_statement;
    END IF;
END$$

DROP PROCEDURE IF EXISTS hf_add_index_if_missing$$
CREATE PROCEDURE hf_add_index_if_missing(IN p_table_name VARCHAR(64), IN p_index_name VARCHAR(64), IN p_index_definition VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table_name AND INDEX_NAME = p_index_name) THEN
        SET @hf_sql = CONCAT('ALTER TABLE `', REPLACE(p_table_name, '`', '``'), '` ADD ', p_index_definition);
        PREPARE hf_statement FROM @hf_sql;
        EXECUTE hf_statement;
        DEALLOCATE PREPARE hf_statement;
    END IF;
END$$

DROP PROCEDURE IF EXISTS hf_drop_index_if_exists$$
CREATE PROCEDURE hf_drop_index_if_exists(IN p_table_name VARCHAR(64), IN p_index_name VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table_name AND INDEX_NAME = p_index_name) THEN
        SET @hf_sql = CONCAT('ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
                '` DROP INDEX `', REPLACE(p_index_name, '`', '``'), '`');
        PREPARE hf_statement FROM @hf_sql;
        EXECUTE hf_statement;
        DEALLOCATE PREPARE hf_statement;
    END IF;
END$$

DROP PROCEDURE IF EXISTS hf_modify_column_if_exists$$
CREATE PROCEDURE hf_modify_column_if_exists(IN p_table_name VARCHAR(64), IN p_column_name VARCHAR(64), IN p_column_definition VARCHAR(1000))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table_name AND COLUMN_NAME = p_column_name) THEN
        SET @hf_sql = CONCAT('ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
                '` MODIFY COLUMN `', REPLACE(p_column_name, '`', '``'), '` ', p_column_definition);
        PREPARE hf_statement FROM @hf_sql;
        EXECUTE hf_statement;
        DEALLOCATE PREPARE hf_statement;
    END IF;
END$$

DROP PROCEDURE IF EXISTS hf_add_job_position_source_index$$
CREATE PROCEDURE hf_add_job_position_source_index()
BEGIN
    IF EXISTS (SELECT 1 FROM job_position WHERE source_sheet IS NOT NULL AND source_row IS NOT NULL
        GROUP BY import_file_id, source_sheet, source_row HAVING COUNT(*) > 1 LIMIT 1) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'job_position 存在重复来源行，请先处理后再创建 uk_job_position_source';
    END IF;
    CALL hf_add_index_if_missing('job_position', 'uk_job_position_source',
            'UNIQUE INDEX uk_job_position_source (import_file_id, source_sheet, source_row)');
END$$

DELIMITER ;

-- 历史表新增字段。
CALL hf_add_column_if_missing('user_profile', 'english_level', 'VARCHAR(16) DEFAULT NULL');
CALL hf_add_column_if_missing('import_file', 'exam_year', 'INT DEFAULT NULL COMMENT ''岗位表考试年度（从原始文件名识别）''');
CALL hf_add_column_if_missing('import_file', 'file_checksum', 'CHAR(64) NULL');
CALL hf_add_column_if_missing('import_file', 'parser_version', 'VARCHAR(32) NULL');
CALL hf_add_column_if_missing('import_file', 'processed_rows', 'INT NOT NULL DEFAULT 0');
CALL hf_add_column_if_missing('import_file', 'success_rows', 'INT NOT NULL DEFAULT 0');
CALL hf_add_column_if_missing('import_file', 'failed_rows', 'INT NOT NULL DEFAULT 0');
CALL hf_add_column_if_missing('import_file', 'error_message', 'VARCHAR(1000) NULL');
CALL hf_add_column_if_missing('job_position', 'department_code', 'VARCHAR(64) NULL');
CALL hf_add_column_if_missing('job_position', 'additional_requirements', 'TEXT NULL');
CALL hf_add_column_if_missing('job_position', 'work_location', 'VARCHAR(500) NULL');
CALL hf_add_column_if_missing('job_position', 'settlement_location', 'VARCHAR(500) NULL');
CALL hf_add_column_if_missing('career_profile', 'current_position', 'VARCHAR(255) DEFAULT NULL');
CALL hf_add_column_if_missing('career_profile', 'total_work_years', 'VARCHAR(64) DEFAULT NULL');
CALL hf_add_column_if_missing('career_profile', 'career_directions', 'TEXT DEFAULT NULL');
CALL hf_add_column_if_missing('career_profile', 'industries', 'TEXT DEFAULT NULL');
CALL hf_add_column_if_missing('job_position_feature', 'major_similarity_keys', 'TEXT DEFAULT NULL');
CALL hf_add_column_if_missing('job_position_feature', 'effective_major_requirement', 'TEXT NULL');
CALL hf_add_column_if_missing('job_position_feature', 'remark_conditions', 'TEXT NULL');
CALL hf_add_column_if_missing('job_position_feature', 'work_location_name', 'VARCHAR(255) NULL');
CALL hf_add_column_if_missing('job_position_feature', 'work_location_code', 'VARCHAR(32) NULL');
CALL hf_add_column_if_missing('job_position_feature', 'work_location_status', 'VARCHAR(16) NOT NULL DEFAULT ''UNKNOWN''');
CALL hf_add_column_if_missing('job_position_feature', 'work_location_source', 'VARCHAR(32) NULL');
CALL hf_add_column_if_missing('job_position_feature', 'work_location_evidence', 'TEXT NULL');
CALL hf_add_column_if_missing('job_position_feature', 'work_location_type', 'VARCHAR(32) NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'detail_status', 'VARCHAR(16) NOT NULL DEFAULT ''DISCOVERED''');
CALL hf_add_column_if_missing('recruitment_notice', 'process_status', 'VARCHAR(16) NOT NULL DEFAULT ''DISCOVERED''');
CALL hf_add_column_if_missing('recruitment_notice', 'process_stage', 'VARCHAR(32) DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'process_failure_code', 'VARCHAR(64) DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'process_failure_reason', 'VARCHAR(500) DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'process_position_count', 'INT NOT NULL DEFAULT 0');
CALL hf_add_column_if_missing('recruitment_notice', 'processed_at', 'DATETIME DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'body_html', 'LONGTEXT DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'body_text', 'LONGTEXT DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'detail_fetched_at', 'DATETIME DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_notice', 'detail_error', 'VARCHAR(500) DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_attachment', 'dedupe_key', 'CHAR(64) NULL');
CALL hf_add_column_if_missing('recruitment_attachment', 'parse_status', 'VARCHAR(16) NOT NULL DEFAULT ''UNPARSED''');
CALL hf_add_column_if_missing('recruitment_attachment', 'parse_error', 'VARCHAR(500) DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_attachment', 'parsed_at', 'DATETIME DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_attachment', 'position_count', 'INT NOT NULL DEFAULT 0');
CALL hf_add_column_if_missing('recruitment_position', 'source_type', 'VARCHAR(32) NOT NULL DEFAULT ''ATTACHMENT_EXCEL''');
CALL hf_add_column_if_missing('recruitment_position', 'extraction_quality', 'VARCHAR(32) NOT NULL DEFAULT ''PARTIAL''');
CALL hf_modify_column_if_exists('recruitment_position', 'source_attachment_id', 'BIGINT DEFAULT NULL');
CALL hf_add_column_if_missing('recruitment_match_result', 'uncertain_reason', 'VARCHAR(64) DEFAULT NULL');

-- 历史数据补齐；只更新原字段为空的记录。
UPDATE import_file SET exam_year = 2024 WHERE exam_year IS NULL AND original_name LIKE '%2024%';
UPDATE import_file SET exam_year = 2025 WHERE exam_year IS NULL AND original_name LIKE '%2025%';
UPDATE import_file SET exam_year = 2026 WHERE exam_year IS NULL AND original_name LIKE '%2026%';
UPDATE import_file SET exam_year = 2027 WHERE exam_year IS NULL AND original_name LIKE '%2027%';
UPDATE recruitment_attachment SET dedupe_key = SHA2(CONCAT(file_url, '\n', file_name), 256)
WHERE dedupe_key IS NULL OR dedupe_key = '';
CALL hf_modify_column_if_exists('recruitment_attachment', 'dedupe_key', 'CHAR(64) NOT NULL');

-- 补齐历史索引。
CALL hf_add_index_if_missing('job_favorite', 'uk_job_favorite_profile_position', 'UNIQUE INDEX uk_job_favorite_profile_position (profile_id, position_id)');
CALL hf_add_index_if_missing('job_favorite', 'idx_job_favorite_profile_created', 'INDEX idx_job_favorite_profile_created (profile_id, created_at)');
CALL hf_add_index_if_missing('job_interview_score', 'uk_interview_score_year_code', 'UNIQUE INDEX uk_interview_score_year_code (exam_year, position_code)');
CALL hf_add_index_if_missing('job_interview_score', 'idx_interview_score_code', 'INDEX idx_interview_score_code (position_code)');
CALL hf_add_index_if_missing('job_position_feature', 'idx_position_feature_subject_group', 'INDEX idx_position_feature_subject_group (exam_subject_group)');
CALL hf_add_index_if_missing('job_position_feature', 'idx_position_feature_major_type', 'INDEX idx_position_feature_major_type (major_restriction_type)');
CALL hf_add_index_if_missing('job_position_feature', 'idx_position_feature_work_location', 'INDEX idx_position_feature_work_location (work_location_status, work_location_code)');
CALL hf_add_index_if_missing('job_preference', 'uk_job_preference_profile', 'UNIQUE INDEX uk_job_preference_profile (profile_id)');
CALL hf_add_index_if_missing('recruitment_notice', 'idx_recruitment_notice_process', 'INDEX idx_recruitment_notice_process (process_status)');
CALL hf_add_index_if_missing('recruitment_attachment', 'uk_recruitment_attachment_notice_dedupe', 'UNIQUE INDEX uk_recruitment_attachment_notice_dedupe (notice_id, dedupe_key)');
CALL hf_drop_index_if_exists('recruitment_attachment', 'uk_recruitment_attachment_notice_url');
CALL hf_add_index_if_missing('recruitment_position', 'uk_recruitment_position_source', 'UNIQUE INDEX uk_recruitment_position_source (source_attachment_id, source_sheet, source_row)');
CALL hf_add_index_if_missing('recruitment_position', 'idx_recruitment_position_notice', 'INDEX idx_recruitment_position_notice (notice_id)');
CALL hf_add_index_if_missing('recruitment_requirement', 'idx_recruitment_requirement_position', 'INDEX idx_recruitment_requirement_position (position_id)');
CALL hf_add_index_if_missing('recruitment_match_result', 'uk_recruitment_match_profile_position', 'UNIQUE INDEX uk_recruitment_match_profile_position (profile_id, position_id)');
CALL hf_add_index_if_missing('recruitment_match_result', 'idx_recruitment_match_profile_result', 'INDEX idx_recruitment_match_profile_result (profile_id, match_result)');
CALL hf_add_index_if_missing('recruitment_match_item', 'idx_recruitment_match_item_result', 'INDEX idx_recruitment_match_item_result (match_result_id)');
CALL hf_add_job_position_source_index();

-- 内置招聘来源，按 source_code 幂等补齐。
INSERT INTO recruitment_source(source_code, source_name, source_type, list_url, enabled, created_at, updated_at)
SELECT 'JIANGXI_SASAC', '江西省国有资产监督管理委员会', 'WEB',
       'https://gzw.jiangxi.gov.cn/jxsgzw/rczp/index.html', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recruitment_source WHERE source_code = 'JIANGXI_SASAC');
INSERT INTO recruitment_source(source_code, source_name, source_type, list_url, enabled, created_at, updated_at)
SELECT 'JIANGXI_TALENT_EVALUATION', '江西省人才测评中心', 'WEB',
       'https://www.jxrcrsksta.com/list/7', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM recruitment_source WHERE source_code = 'JIANGXI_TALENT_EVALUATION');

DROP PROCEDURE IF EXISTS hf_add_job_position_source_index;
DROP PROCEDURE IF EXISTS hf_modify_column_if_exists;
DROP PROCEDURE IF EXISTS hf_drop_index_if_exists;
DROP PROCEDURE IF EXISTS hf_add_index_if_missing;
DROP PROCEDURE IF EXISTS hf_add_column_if_missing;

SELECT 'hf_incremental.sql 执行完成' AS migration_result;
