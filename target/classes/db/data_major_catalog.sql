-- =============================================================
-- 专业目录内置样例初始化数据（Iteration 4）
-- 说明:
--   1. 本文件仅初始化部分计算机相关专业 + 少量法学专业，用于验证
--      目录架构与专业匹配引擎，并不是完整专业目录。
--   2. 数据来源见 docs/major-catalog.md:
--      - 教育部《普通高等学校本科专业目录(2026年)》
--      - 国务院学位委员会 / 教育部《研究生教育学科专业目录(2022年)》
--   3. 后续允许通过官方目录导入补全，禁止声称系统已覆盖全部专业。
--   4. 测试环境通过 spring.sql.init.data-locations 自动执行；
--      生产环境由 migration_iteration4.sql 引用本文件同内容初始化。
-- =============================================================

-- -------------------------------------------------------------
-- 目录 1：教育部本科专业目录（2026年）
-- -------------------------------------------------------------
INSERT INTO major_catalog (id, catalog_code, catalog_name, catalog_type, education_level, version,
                           source_name, source_url, source_year, priority, enabled, created_at, updated_at)
VALUES (1, 'MOE_UNDERGRADUATE_2026', '普通高等学校本科专业目录(2026年)', 'MOE', 'UNDERGRADUATE', '2026',
        '中华人民共和国教育部', NULL, '2026', 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 08 工学（学科门类）
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (101, 1, NULL, '08', '工学', '工学', 'CATEGORY', NULL, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 0809 计算机类（本科专业类）
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (102, 1, 101, '0809', '计算机类', '计算机类', 'CLASS', NULL, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 0809 计算机类下属专业
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

-- 03 法学（学科门类）与 0301 法学类（用于跨类 NOT_MATCH 判定验证，同样为官方真实数据）
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (118, 1, NULL, '03', '法学', '法学', 'CATEGORY', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (119, 1, 118, '0301', '法学类', '法学类', 'CLASS', NULL, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (120, 1, 119, '030101K', '法学', '法学', 'MAJOR', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 人工维护别名：计算机科学技术 -> 计算机科学与技术（常见历史称谓）
INSERT INTO major_alias (id, catalog_id, major_catalog_item_id, alias_name, normalized_alias, alias_type, created_at)
VALUES (301, 1, 103, '计算机科学技术', '计算机科学技术', 'MANUAL', CURRENT_TIMESTAMP);

-- -------------------------------------------------------------
-- 目录 2：研究生教育学科专业目录（2022年）
-- 注意：研究生一级学科与本科"专业类"不是同一层级概念，
--       代码模型不做固定位数假设，层级关系全部由 parent_id 表达。
-- -------------------------------------------------------------
INSERT INTO major_catalog (id, catalog_code, catalog_name, catalog_type, education_level, version,
                           source_name, source_url, source_year, priority, enabled, created_at, updated_at)
VALUES (2, 'MOE_GRADUATE_2022', '研究生教育学科专业目录(2022年)', 'MOE', 'GRADUATE', '2022',
        '国务院学位委员会 / 教育部', NULL, '2022', 110, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 08 工学（学科门类）
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (201, 2, NULL, '08', '工学', '工学', 'CATEGORY', NULL, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 工学门类下的一级学科 / 专业学位类别
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

-- 03 法学（学科门类）与 0301 法学一级学科（官方真实数据）
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (206, 2, NULL, '03', '法学', '法学', 'CATEGORY', NULL, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO major_catalog_item (id, catalog_id, parent_id, major_code, major_name, normalized_name,
                                item_level, degree_category, sort_no, created_at, updated_at)
VALUES (207, 2, 206, '0301', '法学', '法学', 'DISCIPLINE', '学术学位', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
