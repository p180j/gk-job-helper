-- 原始简历文件：单用户 V1 每个档案保留当前一份，职业画像不受该表更新影响。
CREATE TABLE IF NOT EXISTS resume_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    original_filename VARCHAR(512) NOT NULL,
    file_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    uploaded_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_file_profile (profile_id)
);
