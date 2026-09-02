-- Iteration 15：职业概况字段；均保存用户确认后的 AI 草稿内容，不从基础档案或前端推断。
ALTER TABLE career_profile ADD COLUMN current_position VARCHAR(255) DEFAULT NULL AFTER profile_id;
ALTER TABLE career_profile ADD COLUMN total_work_years VARCHAR(64) DEFAULT NULL AFTER current_position;
ALTER TABLE career_profile ADD COLUMN career_directions TEXT DEFAULT NULL AFTER total_work_years;
ALTER TABLE career_profile ADD COLUMN industries TEXT DEFAULT NULL AFTER career_directions;
