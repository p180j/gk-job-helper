-- =============================================================
-- Iteration 2 -> Iteration 3 迁移脚本（存量 MySQL 库直接执行）
-- 内容：按匹配引擎 V1 重新设计 job_match / job_match_item
--
-- 说明：
-- 1. Iteration 2 未实现匹配逻辑，job_match / job_match_item 无业务数据，
--    可安全重建（DROP + CREATE）。如自行写入过测试数据请先备份。
-- 2. 本次不改动 user_profile / job_position / import_file 等已有表。
-- 3. 全新部署无需执行本脚本，直接执行 db/schema.sql 即可。
-- =============================================================

USE gk_job_helper;

-- 1. 重建 job_match：从"批量任务汇总"改为"档案+岗位"粒度的最新匹配结果
DROP TABLE IF EXISTS job_match_item;
DROP TABLE IF EXISTS job_match;

CREATE TABLE job_match (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    profile_id       BIGINT       NOT NULL COMMENT '档案 -> user_profile.id',
    job_position_id  BIGINT       NOT NULL COMMENT '岗位 -> job_position.id',
    import_file_id   BIGINT       DEFAULT NULL COMMENT '来源文件 -> import_file.id',
    match_result     VARCHAR(16)  NOT NULL COMMENT 'MATCH / UNCERTAIN / NOT_MATCH',
    reference_date   DATE         DEFAULT NULL COMMENT '匹配基准日期',
    created_at       DATETIME     DEFAULT NULL,
    updated_at       DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_match_profile_position (profile_id, job_position_id),
    KEY idx_job_match_query (profile_id, import_file_id, match_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位匹配结果';

CREATE TABLE job_match_item (
    id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    job_match_id       BIGINT        NOT NULL COMMENT '匹配结果 -> job_match.id',
    job_position_id    BIGINT        NOT NULL COMMENT '岗位 -> job_position.id',
    condition_type     VARCHAR(32)   NOT NULL COMMENT 'EDUCATION/AGE/POLITICAL/WORK_EXPERIENCE',
    match_result       VARCHAR(16)   NOT NULL COMMENT 'MATCH / UNCERTAIN / NOT_MATCH',
    user_value         VARCHAR(255)  DEFAULT NULL COMMENT '用户档案对应值',
    requirement_value  VARCHAR(500)  DEFAULT NULL COMMENT '岗位要求值',
    reason             VARCHAR(1000) DEFAULT NULL COMMENT '中文可读判定原因',
    created_at         DATETIME      DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_job_match_item_match (job_match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位匹配条件明细';
