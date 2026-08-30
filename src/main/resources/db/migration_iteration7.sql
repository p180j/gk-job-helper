-- Iteration 7：岗位收藏
-- 适用：已有 gk_job_helper 数据库；不删除、不重建任何现有业务表。

CREATE TABLE IF NOT EXISTS job_favorite (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    profile_id   BIGINT   NOT NULL,
    position_id  BIGINT   NOT NULL,
    created_at   DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_job_favorite_profile FOREIGN KEY (profile_id) REFERENCES user_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_favorite_position FOREIGN KEY (position_id) REFERENCES job_position (id) ON DELETE CASCADE,
    CONSTRAINT uk_job_favorite_profile_position UNIQUE (profile_id, position_id),
    INDEX idx_job_favorite_profile_created (profile_id, created_at)
);
