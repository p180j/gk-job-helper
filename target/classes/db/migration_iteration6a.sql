-- Iteration 6A 增量迁移：禁止 DROP，保留既有档案、职位表与匹配结果。

ALTER TABLE exam ADD COLUMN exam_code VARCHAR(64) DEFAULT NULL;
ALTER TABLE exam ADD COLUMN exam_name VARCHAR(128) DEFAULT NULL;
ALTER TABLE exam ADD COLUMN province VARCHAR(32) DEFAULT NULL;
ALTER TABLE exam ADD COLUMN year INT DEFAULT NULL;
ALTER TABLE exam ADD COLUMN source_name VARCHAR(255) DEFAULT NULL;
ALTER TABLE exam ADD COLUMN source_url VARCHAR(512) DEFAULT NULL;
UPDATE exam SET exam_name = name WHERE exam_name IS NULL;
CREATE UNIQUE INDEX uk_exam_code ON exam (exam_code);

ALTER TABLE import_file ADD COLUMN exam_id BIGINT DEFAULT NULL;
ALTER TABLE import_file ADD COLUMN processed_rows INT DEFAULT 0;
ALTER TABLE import_file ADD COLUMN success_rows INT DEFAULT 0;
ALTER TABLE import_file ADD COLUMN failed_rows INT DEFAULT 0;
ALTER TABLE import_file ADD COLUMN error_message VARCHAR(1000) DEFAULT NULL;
CREATE INDEX idx_import_file_exam ON import_file (exam_id);

CREATE TABLE IF NOT EXISTS user_education (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    education_level VARCHAR(32) NOT NULL,
    degree VARCHAR(32) DEFAULT NULL,
    school_name VARCHAR(255) DEFAULT NULL,
    major_name VARCHAR(128) DEFAULT NULL,
    major_code VARCHAR(128) DEFAULT NULL,
    graduation_date DATE DEFAULT NULL,
    is_highest TINYINT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_user_education_profile ON user_education (profile_id, enabled);

INSERT INTO user_education (profile_id, education_level, degree, major_name, major_code, graduation_date,
                            is_highest, enabled, created_at, updated_at)
SELECT id, COALESCE(major_education_level, education), degree, major, major_code, graduation_date,
       1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user_profile
WHERE major IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_education e WHERE e.profile_id = user_profile.id);

CREATE TABLE IF NOT EXISTS major_correspondence (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_catalog_id BIGINT NOT NULL,
    source_major_code VARCHAR(32) DEFAULT NULL,
    source_major_name VARCHAR(255) NOT NULL,
    target_catalog_id BIGINT NOT NULL,
    target_major_code VARCHAR(32) DEFAULT NULL,
    target_major_name VARCHAR(255) NOT NULL,
    relation_type VARCHAR(16) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_major_correspondence_source ON major_correspondence (source_catalog_id, source_major_code);
CREATE INDEX idx_major_correspondence_target ON major_correspondence (target_catalog_id, target_major_code);

INSERT INTO exam (exam_code, exam_name, name, exam_type, province, year, source_name, created_at, updated_at)
SELECT 'JIANGXI_CIVIL_2026', '江西省2026年度考试录用公务员', '江西省2026年度考试录用公务员',
       'CIVIL_SERVICE', '江西', 2026, '江西省公务员主管部门', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM exam WHERE exam_code = 'JIANGXI_CIVIL_2026');
