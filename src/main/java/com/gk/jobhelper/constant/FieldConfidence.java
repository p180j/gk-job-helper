package com.gk.jobhelper.constant;

/**
 * 字段映射建议置信度
 * EXACT   - 表头与标准字段名精确一致
 * ALIAS   - 表头命中同义词
 * UNKNOWN - 无法识别（需用户人工指定目标字段）
 */
public enum FieldConfidence {
    EXACT,
    ALIAS,
    UNKNOWN
}
