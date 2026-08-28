-- =============================================================
-- 公考智能选岗助手建表脚本（Iteration 4）
-- 适用: MySQL 5.7+ / 8.0 （同样兼容单元测试使用的 H2 MySQL 模式）
-- 全新部署执行前请先创建数据库:
--   CREATE DATABASE gk_job_helper DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
--   USE gk_job_helper;
-- 存量库升级(Iteration 3 -> 4)请执行 db/migration_iteration4.sql
-- 专业目录内置样例数据见 db/data_major_catalog.sql（数据来源说明见 docs/major-catalog.md）
-- =============================================================

-- 用户个人档案（Iteration 1 单档案模式；Iteration 2 补充 8 个档案字段）
CREATE TABLE IF NOT EXISTS user_profile (
    id                    BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    name                  VARCHAR(64)   NOT NULL,                -- 姓名
    gender                VARCHAR(8)    DEFAULT NULL,            -- 性别
    birth_date            DATE          DEFAULT NULL,            -- 出生日期
    political_status      VARCHAR(32)   DEFAULT NULL,            -- 政治面貌
    education             VARCHAR(32)   DEFAULT NULL,            -- 学历
    degree                VARCHAR(32)   DEFAULT NULL,            -- 学位
    major                 VARCHAR(128)  DEFAULT NULL,            -- 专业
    major_code            VARCHAR(128)  DEFAULT NULL,            -- 专业代码
    major_education_level VARCHAR(32)   DEFAULT NULL,            -- 专业对应学历层次(本科/研究生/专科，空则按 education 推断，Iteration 4)
    graduation_date       DATE          DEFAULT NULL,            -- 毕业时间
    work_years            INT           DEFAULT NULL,            -- 工作年限（基层工作经历年限）
    fresh_graduate_status VARCHAR(32)   DEFAULT NULL,            -- 应届生身份(是/否)
    household             VARCHAR(128)  DEFAULT NULL,            -- 户籍
    student_origin        VARCHAR(128)  DEFAULT NULL,            -- 生源地
    service_project_type  VARCHAR(64)   DEFAULT NULL,            -- 服务基层项目类型
    veteran               VARCHAR(16)   DEFAULT NULL,            -- 退役军人(是/否)
    certificates          VARCHAR(500)  DEFAULT NULL,            -- 持有证书(逗号分隔)
    target_region         VARCHAR(128)  DEFAULT NULL,            -- 目标报考地区
    notes                 VARCHAR(1000) DEFAULT NULL,            -- 备注
    created_at            DATETIME      DEFAULT NULL,            -- 创建时间
    updated_at            DATETIME      DEFAULT NULL,            -- 更新时间
    PRIMARY KEY (id)
);

-- Iteration 6A：教育经历；保留 user_profile 中旧学历/专业字段作为兼容回退。
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

-- 考试（一次岗位表导入对应一场考试，后续迭代使用）
CREATE TABLE IF NOT EXISTS exam (
    id               BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    name             VARCHAR(128)  NOT NULL,                -- 考试名称，如 2026国考
    exam_type        VARCHAR(32)   DEFAULT NULL,            -- 考试类型: 国考/省考/事业编等
    exam_date        DATE          DEFAULT NULL,            -- 考试日期
    recruit_count    INT           DEFAULT NULL,            -- 招录总人数
    source_file_id   BIGINT        DEFAULT NULL,            -- 来源导入文件 id -> import_file.id
    created_at       DATETIME      DEFAULT NULL,
    updated_at       DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 导入文件记录（每次 Excel 上传均记录，保留原始表头与行数用于字段映射）
CREATE TABLE IF NOT EXISTS import_file (
    id               BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    original_name    VARCHAR(255)  NOT NULL,                -- 上传时的原始文件名
    stored_name      VARCHAR(255)  NOT NULL,                -- 本地保存后的文件名
    stored_path      VARCHAR(512)  NOT NULL,                -- 本地保存完整路径
    file_size        BIGINT        DEFAULT NULL,            -- 文件大小(字节)
    file_type        VARCHAR(8)    DEFAULT NULL,            -- 文件类型: .xls / .xlsx
    sheet_name       VARCHAR(128)  DEFAULT NULL,            -- 解析出的 Sheet 名称
    headers          VARCHAR(1000) DEFAULT NULL,            -- 表头字段(逗号分隔)，供字段映射使用
    total_rows       INT           DEFAULT NULL,            -- 数据总行数(不含表头)
    status           VARCHAR(16)   NOT NULL DEFAULT 'PREVIEWED', -- PREVIEWED-已预览 / IMPORTED-已导入
    processed_rows   INT           DEFAULT 0,
    success_rows     INT           DEFAULT 0,
    failed_rows      INT           DEFAULT 0,
    error_message    VARCHAR(1000) DEFAULT NULL,
    created_at       DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 岗位表（Iteration 2 完整标准模型）
CREATE TABLE IF NOT EXISTS job_position (
    id                         BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    exam_id                    BIGINT        DEFAULT NULL,       -- 所属考试 -> exam.id
    import_file_id             BIGINT        DEFAULT NULL,       -- 来源文件 -> import_file.id
    department_name            VARCHAR(255)  DEFAULT NULL,       -- 招录机关/部门
    organization_name          VARCHAR(255)  DEFAULT NULL,       -- 用人司局/单位
    position_name              VARCHAR(255)  DEFAULT NULL,       -- 职位名称
    position_code              VARCHAR(64)   DEFAULT NULL,       -- 职位代码
    province                   VARCHAR(64)   DEFAULT NULL,       -- 省份
    city                       VARCHAR(64)   DEFAULT NULL,       -- 城市
    district                   VARCHAR(64)   DEFAULT NULL,       -- 区县
    recruit_count              INT           DEFAULT NULL,       -- 招考人数
    education_requirement      VARCHAR(255)  DEFAULT NULL,       -- 学历要求
    degree_requirement         VARCHAR(255)  DEFAULT NULL,       -- 学位要求
    major_requirement          VARCHAR(2000) DEFAULT NULL,       -- 专业要求
    major_codes                VARCHAR(1000) DEFAULT NULL,       -- 专业代码
    age_requirement            VARCHAR(64)   DEFAULT NULL,       -- 年龄要求
    political_requirement      VARCHAR(128)  DEFAULT NULL,       -- 政治面貌要求
    work_year_requirement      VARCHAR(64)   DEFAULT NULL,       -- 基层工作年限要求
    fresh_graduate_requirement VARCHAR(64)   DEFAULT NULL,       -- 应届生要求
    household_requirement      VARCHAR(255)  DEFAULT NULL,       -- 户籍要求
    service_project_requirement VARCHAR(255) DEFAULT NULL,       -- 服务基层项目要求
    certificate_requirement    VARCHAR(255)  DEFAULT NULL,       -- 证书要求
    gender_requirement         VARCHAR(32)   DEFAULT NULL,       -- 性别要求
    position_description       VARCHAR(2000) DEFAULT NULL,       -- 职位描述
    remark                     VARCHAR(2000) DEFAULT NULL,       -- 备注/其他条件
    source_sheet               VARCHAR(128)  DEFAULT NULL,       -- 来源 Sheet 名称
    source_row                 INT           DEFAULT NULL,       -- 来源 Excel 行号(表头为第 1 行，数据行从 2 起)
    raw_data                   TEXT,                             -- Excel 原始整行 JSON(表头->值)，不丢弃任何原始字段
    created_at                 DATETIME      DEFAULT NULL,
    updated_at                 DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 岗位表必要索引（控制在最少必要范围）
CREATE INDEX idx_job_position_import        ON job_position (import_file_id);
CREATE INDEX idx_job_position_code          ON job_position (position_code);
CREATE INDEX idx_job_position_department    ON job_position (department_name);
CREATE INDEX idx_job_position_organization  ON job_position (organization_name);
CREATE INDEX idx_job_position_region        ON job_position (province, city);

-- 岗位匹配结果（Iteration 3：一个档案 + 一个岗位保留最新匹配结果，重复匹配覆盖更新）
-- match_result: 综合匹配结果 MATCH / UNCERTAIN / NOT_MATCH
CREATE TABLE IF NOT EXISTS job_match (
    id               BIGINT       NOT NULL AUTO_INCREMENT, -- 主键
    profile_id       BIGINT       NOT NULL,                -- 档案 -> user_profile.id
    job_position_id  BIGINT       NOT NULL,                -- 岗位 -> job_position.id
    import_file_id   BIGINT       DEFAULT NULL,            -- 来源文件 -> import_file.id（冗余，便于按导入批次查询）
    match_result     VARCHAR(16)  NOT NULL,                -- MATCH / UNCERTAIN / NOT_MATCH
    reference_date   DATE         DEFAULT NULL,            -- 匹配基准日期（年龄等条件计算基准）
    created_at       DATETIME     DEFAULT NULL,
    updated_at       DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_job_match_profile_position ON job_match (profile_id, job_position_id);
CREATE INDEX idx_job_match_query ON job_match (profile_id, import_file_id, match_result);

-- 岗位匹配条件明细（每个条件类型一条记录）
-- condition_type: 条件类型(EDUCATION/AGE/POLITICAL/WORK_EXPERIENCE/MAJOR，后续扩展)
-- match_result:   单条件结果 MATCH / UNCERTAIN / NOT_MATCH
CREATE TABLE IF NOT EXISTS job_match_item (
    id                 BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    job_match_id       BIGINT        NOT NULL,                -- 匹配结果 -> job_match.id
    job_position_id    BIGINT        NOT NULL,                -- 岗位 -> job_position.id（冗余）
    condition_type     VARCHAR(32)   NOT NULL,                -- 条件类型
    match_result       VARCHAR(16)   NOT NULL,                -- MATCH / UNCERTAIN / NOT_MATCH
    user_value         VARCHAR(255)  DEFAULT NULL,            -- 用户档案对应值
    requirement_value  VARCHAR(500)  DEFAULT NULL,            -- 岗位要求值
    reason             VARCHAR(1000) DEFAULT NULL,            -- 中文可读判定原因
    evidence           VARCHAR(1000) DEFAULT NULL,            -- 匹配证据 JSON(专业目录来源等，Iteration 4)
    created_at         DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_job_match_item_match ON job_match_item (job_match_id);

-- =============================================================
-- 专业目录体系（Iteration 4）：可版本化、可追溯、可扩展
-- catalog_type:    MOE(教育部目录) / EXAM(考试专用目录) / AGENCY(招录单位目录) / CUSTOM(自定义)
-- education_level: UNDERGRADUATE(本科) / GRADUATE(研究生) / VOCATIONAL(专科) / MIXED(混合)
-- priority:        数字越小优先级越高（目录选择时使用）
-- =============================================================
CREATE TABLE IF NOT EXISTS major_catalog (
    id               BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    catalog_code     VARCHAR(64)   NOT NULL,                -- 目录编码，如 MOE_UNDERGRADUATE_2026
    catalog_name     VARCHAR(255)  NOT NULL,                -- 目录名称，如 普通高等学校本科专业目录(2026年)
    catalog_type     VARCHAR(16)   NOT NULL,                -- MOE / EXAM / AGENCY / CUSTOM
    education_level  VARCHAR(16)   NOT NULL,                -- UNDERGRADUATE / GRADUATE / VOCATIONAL / MIXED
    version          VARCHAR(32)   DEFAULT NULL,            -- 目录版本(年份等)
    source_name      VARCHAR(255)  DEFAULT NULL,            -- 来源单位名称
    source_url       VARCHAR(512)  DEFAULT NULL,            -- 来源官方链接
    source_year      VARCHAR(16)   DEFAULT NULL,            -- 来源年份
    priority         INT           NOT NULL DEFAULT 100,    -- 优先级(越小越高)
    enabled          TINYINT       NOT NULL DEFAULT 1,      -- 是否启用(1/0)
    created_at       DATETIME      DEFAULT NULL,
    updated_at       DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_major_catalog_code ON major_catalog (catalog_code);

-- 专业目录节点（父子树结构，不把类->专业关系硬编码在代码中）
-- item_level: CATEGORY(学科门类) / CLASS(本科专业类) / MAJOR(本科专业) /
--             DISCIPLINE(研究生一级学科) / FIELD(专业学位类别) / OTHER
CREATE TABLE IF NOT EXISTS major_catalog_item (
    id              BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    catalog_id      BIGINT        NOT NULL,                -- 所属目录 -> major_catalog.id
    parent_id       BIGINT        DEFAULT NULL,            -- 父节点 -> major_catalog_item.id
    major_code      VARCHAR(32)   DEFAULT NULL,            -- 专业/类代码，如 080902 / 0809 / 0812
    major_name      VARCHAR(255)  NOT NULL,                -- 节点名称
    normalized_name VARCHAR(255)  DEFAULT NULL,            -- 标准化名称(比较用)
    item_level      VARCHAR(16)   NOT NULL,                -- CATEGORY / CLASS / MAJOR / DISCIPLINE / FIELD / OTHER
    degree_category VARCHAR(64)   DEFAULT NULL,            -- 学位类别(研究生目录用：学术学位/专业学位)
    sort_no         INT           DEFAULT NULL,            -- 排序号
    raw_data        VARCHAR(1000) DEFAULT NULL,            -- 原始行 JSON(来源导入时保留)
    created_at      DATETIME      DEFAULT NULL,
    updated_at      DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_mci_catalog_code ON major_catalog_item (catalog_id, major_code);
CREATE INDEX idx_mci_catalog_name ON major_catalog_item (catalog_id, normalized_name);
CREATE INDEX idx_mci_parent ON major_catalog_item (catalog_id, parent_id);

-- 专业别名（仅允许来源明确或人工维护的别名，禁止自动相似推断）
-- alias_type: OFFICIAL(官方曾用名/目录内别称) / MANUAL(人工维护)
CREATE TABLE IF NOT EXISTS major_alias (
    id                    BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    catalog_id            BIGINT        NOT NULL,                -- 所属目录 -> major_catalog.id
    major_catalog_item_id BIGINT        NOT NULL,                -- 指向目录节点 -> major_catalog_item.id
    alias_name            VARCHAR(255)  NOT NULL,                -- 别名原始值
    normalized_alias      VARCHAR(255)  NOT NULL,                -- 标准化别名(比较用)
    alias_type            VARCHAR(16)   DEFAULT 'MANUAL',        -- OFFICIAL / MANUAL
    created_at            DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_alias_catalog_norm ON major_alias (catalog_id, normalized_alias);
CREATE INDEX idx_alias_item ON major_alias (major_catalog_item_id);

-- 考试与专业目录绑定（一个考试可绑定多个目录，priority 越小优先级越高）
CREATE TABLE IF NOT EXISTS exam_major_catalog (
    id           BIGINT   NOT NULL AUTO_INCREMENT, -- 主键
    exam_id      BIGINT   NOT NULL,                -- 考试 -> exam.id
    catalog_id   BIGINT   NOT NULL,                -- 目录 -> major_catalog.id
    priority     INT      NOT NULL DEFAULT 100,    -- 绑定优先级(越小越高)
    created_at   DATETIME DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_emc_exam ON exam_major_catalog (exam_id, priority);

-- 用户行为记录（后续迭代使用）
CREATE TABLE IF NOT EXISTS job_action (
    id               BIGINT        NOT NULL AUTO_INCREMENT, -- 主键
    user_profile_id  BIGINT        DEFAULT NULL,            -- 档案 -> user_profile.id
    job_position_id  BIGINT        DEFAULT NULL,            -- 岗位 -> job_position.id
    action_type      VARCHAR(32)   NOT NULL,                -- 行为类型: VIEW/FAVORITE/APPLY 等
    action_time      DATETIME      DEFAULT NULL,            -- 行为发生时间
    remark           VARCHAR(500)  DEFAULT NULL,            -- 备注
    created_at       DATETIME      DEFAULT NULL,
    PRIMARY KEY (id)
);
