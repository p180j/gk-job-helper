-- Iteration 8：为我优选 + 进面参考（仅新增三张表，不删除现有数据）
CREATE TABLE IF NOT EXISTS job_interview_score (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_year INT NOT NULL,
    position_code VARCHAR(64) NOT NULL,
    department_name VARCHAR(255) DEFAULT NULL,
    position_name VARCHAR(255) DEFAULT NULL,
    min_interview_score DECIMAL(8,2) NOT NULL,
    interview_candidate_count INT NOT NULL,
    source_file_name VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_interview_score_year_code ON job_interview_score (exam_year, position_code);
CREATE INDEX idx_interview_score_code ON job_interview_score (position_code);

CREATE TABLE IF NOT EXISTS job_position_feature (
    position_id BIGINT NOT NULL,
    exam_subject_count INT DEFAULT NULL,
    exam_subjects TEXT,
    exam_subject_group VARCHAR(512) DEFAULT NULL,
    exam_subject_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    raw_exam_subject_text VARCHAR(1000) DEFAULT NULL,
    major_restriction_type VARCHAR(32) NOT NULL DEFAULT 'UNCERTAIN',
    major_domains TEXT,
    major_similarity_keys TEXT,
    major_scope_count INT DEFAULT NULL,
    major_analysis_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    organization_level VARCHAR(32) DEFAULT NULL,
    organization_level_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (position_id),
    CONSTRAINT fk_job_position_feature_position FOREIGN KEY (position_id) REFERENCES job_position (id) ON DELETE CASCADE
);
CREATE INDEX idx_position_feature_subject_group ON job_position_feature (exam_subject_group);
CREATE INDEX idx_position_feature_major_type ON job_position_feature (major_restriction_type);

CREATE TABLE IF NOT EXISTS job_preference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    preferred_regions TEXT,
    accepted_org_levels TEXT,
    excluded_org_levels TEXT,
    preferred_subject_groups TEXT,
    accept_extra_subjects TINYINT NOT NULL DEFAULT 1,
    prefer_more_recruits TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_job_preference_profile FOREIGN KEY (profile_id) REFERENCES user_profile (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX uk_job_preference_profile ON job_preference (profile_id);
