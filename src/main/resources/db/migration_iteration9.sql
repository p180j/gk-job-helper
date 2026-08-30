-- Iteration 9：岗位表上下文与考试年度
ALTER TABLE import_file
    ADD COLUMN exam_year INT DEFAULT NULL COMMENT '岗位表考试年度（从原始文件名识别）' AFTER total_rows;

-- 兼容已经导入的常用年度职位表；新上传文件由应用自动写入年度。
UPDATE import_file SET exam_year = 2024 WHERE exam_year IS NULL AND original_name LIKE '%2024%';
UPDATE import_file SET exam_year = 2025 WHERE exam_year IS NULL AND original_name LIKE '%2025%';
UPDATE import_file SET exam_year = 2026 WHERE exam_year IS NULL AND original_name LIKE '%2026%';
