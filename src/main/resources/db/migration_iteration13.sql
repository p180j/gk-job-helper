-- Iteration 13：档案中的英语等级。NULL 表示历史档案尚未选择，不把未知误判为未通过。
ALTER TABLE user_profile ADD COLUMN english_level VARCHAR(16) DEFAULT NULL AFTER certificates;
