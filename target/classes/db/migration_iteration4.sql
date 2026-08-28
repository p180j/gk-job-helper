-- =============================================================
-- Iteration 3 -> Iteration 4 迁移脚本
-- 适用: 已完成 Iteration 1~3 的存量 MySQL 数据库
-- 约束: 不删除任何现有业务表(user_profile/job_position/job_match/job_match_item 等)
-- 注意: ALTER TABLE 语句请只执行一次；重复执行会因列已存在而报错，属预期行为。
-- 全新数据库直接执行 db/schema.sql + db/data_major_catalog.sql 即可。
-- =============================================================

USE gk_job_helper;

-- -------------------------------------------------------------
-- 1. 新增专业目录体系表
-- -------------------------------------------------------------

-- 专业目录（catalog_type: MOE/EXAM/AGENCY/CUSTOM; education_level: UNDERGRADUATE/GRADUATE/VOCATIONAL/MIXED）
CREATE TABLE IF NOT EXISTS major_catalog (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    catalog_code     VARCHAR(64)   NOT NULL                COMMENT '目录编码，如 MOE_UNDERGRADUATE_2026',
    catalog_name     VARCHAR(255)  NOT NULL                COMMENT '目录名称',
    catalog_type     VARCHAR(16)   NOT NULL                COMMENT 'MOE/EXAM/AGENCY/CUSTOM',
    education_level  VARCHAR(16)   NOT NULL                COMMENT 'UNDERGRADUATE/GRADUATE/VOCATIONAL/MIXED',
    version          VARCHAR(32)   DEFAULT NULL            COMMENT '目录版本',
    source_name      VARCHAR(255)  DEFAULT NULL            COMMENT '来源单位名称',
    source_url       VARCHAR(512)  DEFAULT NULL            COMMENT '来源官方链接',
    source_year      VARCHAR(16)   DEFAULT NULL            COMMENT '来源年份',
    priority         INT           NOT NULL DEFAULT 100    COMMENT '优先级(越小越高)',
    enabled          TINYINT       NOT NULL DEFAULT 1      COMMENT '是否启用(1/0)',
    created_at       DATETIME      DEFAULT NULL,
    updated_at       DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业目录';

CREATE UNIQUE INDEX uk_major_catalog_code ON major_catalog (catalog_code);

-- 专业目录节点（父子树结构）
-- item_level: CATEGORY/CLASS/MAJOR/DISCIPLINE/FIELD/OTHER
CREATE TABLE IF NOT EXISTS major_catalog_item (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    catalog_id      BIGINT        NOT NULL                COMMENT '所属目录 -> major_catalog.id',
    parent_id       BIGINT        DEFAULT NULL            COMMENT '父节点 -> major_catalog_item.id',
    major_code      VARCHAR(32)   DEFAULT NULL            COMMENT '专业/类代码',
    major_name      VARCHAR(255)  NOT NULL                COMMENT '节点名称',
    normalized_name VARCHAR(255)  DEFAULT NULL            COMMENT '标准化名称(比较用)',
    item_level      VARCHAR(16)   NOT NULL                COMMENT 'CATEGORY/CLASS/MAJOR/DISCIPLINE/FIELD/OTHER',
    degree_category VARCHAR(64)   DEFAULT NULL            COMMENT '学位类别(学术学位/专业学位)',
    sort_no         INT           DEFAULT NULL            COMMENT '排序号',
    raw_data        VARCHAR(1000) DEFAULT NULL            COMMENT '原始行 JSON',
    created_at      DATETIME      DEFAULT NULL,
    updated_at      DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业目录节点';

CREATE INDEX idx_mci_catalog_code ON major_catalog_item (catalog_id, major_code);
CREATE INDEX idx_mci_catalog_name ON major_catalog_item (catalog_id, normalized_name);
CREATE INDEX idx_mci_parent ON major_catalog_item (catalog_id, parent_id);

-- 专业别名（仅来源明确或人工维护）
CREATE TABLE IF NOT EXISTS major_alias (
    id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    catalog_id            BIGINT        NOT NULL                COMMENT '所属目录 -> major_catalog.id',
    major_catalog_item_id BIGINT        NOT NULL                COMMENT '指向目录节点 -> major_catalog_item.id',
    alias_name            VARCHAR(255)  NOT NULL                COMMENT '别名原始值',
    normalized_alias      VARCHAR(255)  NOT NULL                COMMENT '标准化别名(比较用)',
    alias_type            VARCHAR(16)   DEFAULT 'MANUAL'        COMMENT 'OFFICIAL/MANUAL',
    created_at            DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业别名';

CREATE INDEX idx_alias_catalog_norm ON major_alias (catalog_id, normalized_alias);
CREATE INDEX idx_alias_item ON major_alias (major_catalog_item_id);

-- 考试与专业目录绑定
CREATE TABLE IF NOT EXISTS exam_major_catalog (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    exam_id      BIGINT   NOT NULL                COMMENT '考试 -> exam.id',
    catalog_id   BIGINT   NOT NULL                COMMENT '目录 -> major_catalog.id',
    priority     INT      NOT NULL DEFAULT 100    COMMENT '绑定优先级(越小越高)',
    created_at   DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试专业目录绑定';

CREATE INDEX idx_emc_exam ON exam_major_catalog (exam_id, priority);

-- -------------------------------------------------------------
-- 2. 现有表兼容式扩展（不破坏 Iteration 1~3 数据）
-- -------------------------------------------------------------

-- 用户档案：补充专业对应学历层次（空则按 education 推断，第一阶段仍只用最高学历对应专业）
ALTER TABLE user_profile ADD COLUMN major_education_level VARCHAR(32) DEFAULT NULL COMMENT '专业对应学历层次(本科/研究生/专科)';

-- 匹配明细：补充结构化匹配证据 JSON（专业目录来源等）
ALTER TABLE job_match_item ADD COLUMN evidence VARCHAR(1000) DEFAULT NULL COMMENT '匹配证据 JSON';

-- -------------------------------------------------------------
-- 3. 内置样例初始化数据
--    （与 db/data_major_catalog.sql 内容一致，来源说明见 docs/major-catalog.md）
--    仅初始化计算机类等部分专业用于架构验证，并非完整专业目录。
-- -------------------------------------------------------------

INSERT INTO major_catalog (id, catalog_code, catalog_name, catalog_type, education_level, version,
                           source_name, source_url, source_year, priority, enabled, created_at, updated_at)
VALUES (1, 'MOE_UNDERGRADUATE_2026', '普通高等学校本科专业目录(2026年)', 'MOE', 'UNDERGRADUATE', '2026',
        '中华人民共和国教育部', NULL, '2026', 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (101, 1, NULL, '08', '工学', '工学', 'CATEGORY', NULL, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (102, 1, 101, '0809', '计算机类', '计算机类', 'CLASS', NULL, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (103, 1, 102, '080901', '计算机科学与技术', '计算机科学与技术', 'MAJOR', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (104, 1, 102, '080902', '软件工程', '软件工程', 'MAJOR', NULL, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (105, 1, 102, '080903', '网络工程', '网络工程', 'MAJOR', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (106, 1, 102, '080904K', '信息安全', '信息安全', 'MAJOR', NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (107, 1, 102, '080905', '物联网工程', '物联网工程', 'MAJOR', NULL, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (108, 1, 102, '080906', '数字媒体技术', '数字媒体技术', 'MAJOR', NULL, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (109, 1, 102, '080907T', '智能科学与技术', '智能科学与技术', 'MAJOR', NULL, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (110, 1, 102, '080908T', '空间信息与数字技术', '空间信息与数字技术', 'MAJOR', NULL, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (111, 1, 102, '080909T', '电子与计算机工程', '电子与计算机工程', 'MAJOR', NULL, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (112, 1, 102, '080910T', '数据科学与大数据技术', '数据科学与大数据技术', 'MAJOR', NULL, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (113, 1, 102, '080911TK', '网络空间安全', '网络空间安全', 'MAJOR', NULL, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (114, 1, 102, '080916T', '虚拟现实技术', '虚拟现实技术', 'MAJOR', NULL, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (115, 1, 102, '080917T', '区块链工程', '区块链工程', 'MAJOR', NULL, 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (116, 1, 102, '080918TK', '密码科学与技术', '密码科学与技术', 'MAJOR', NULL, 14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (117, 1, 102, '080919T', '工业软件', '工业软件', 'MAJOR', NULL, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (118, 1, NULL, '03', '法学', '法学', 'CATEGORY', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (119, 1, 118, '0301', '法学类', '法学类', 'CLASS', NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (120, 1, 119, '030101K', '法学', '法学', 'MAJOR', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO major_alias (id, catalog_id, major_catalog_item_id, alias_name, normalized_alias, alias_type, created_at)
VALUES (301, 1, 103, '计算机科学技术', '计算机科学技术', 'MANUAL', CURRENT_TIMESTAMP);

INSERT INTO major_catalog (id, catalog_code, catalog_name, catalog_type, education_level, version,
                           source_name, source_url, source_year, priority, enabled, created_at, updated_at)
VALUES (2, 'MOE_GRADUATE_2022', '研究生教育学科专业目录(2022年)', 'MOE', 'GRADUATE', '2022',
        '国务院学位委员会 / 教育部', NULL, '2022', 110, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (201, 2, NULL, '08', '工学', '工学', 'CATEGORY', NULL, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (202, 2, 201, '0812', '计算机科学与技术', '计算机科学与技术', 'DISCIPLINE', '学术学位', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (203, 2, 201, '0835', '软件工程', '软件工程', 'DISCIPLINE', '学术学位', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (204, 2, 201, '0839', '网络空间安全', '网络空间安全', 'DISCIPLINE', '学术学位', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (205, 2, 201, '0854', '电子信息', '电子信息', 'FIELD', '专业学位', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (206, 2, NULL, '03', '法学', '法学', 'CATEGORY', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (207, 2, 206, '0301', '法学', '法学', 'DISCIPLINE', '学术学位', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
