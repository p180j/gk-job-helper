-- Iteration 14：招聘职业画像。简历原文件、原文和 AI 草稿均不入库。
CREATE TABLE career_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    profile_id BIGINT NOT NULL,
    education_experiences TEXT DEFAULT NULL,
    work_experiences TEXT DEFAULT NULL,
    project_experiences TEXT DEFAULT NULL,
    skills TEXT DEFAULT NULL,
    certificates TEXT DEFAULT NULL,
    created_at DATETIME DEFAULT NULL,
    updated_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_career_profile_profile (profile_id)
);
