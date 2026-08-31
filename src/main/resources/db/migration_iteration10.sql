-- Iteration 10：专业相似锚点（仅用于历史进面分同类岗位比较）
ALTER TABLE job_position_feature
    ADD COLUMN major_similarity_keys TEXT DEFAULT NULL AFTER major_domains;
