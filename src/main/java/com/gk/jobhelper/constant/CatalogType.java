package com.gk.jobhelper.constant;

/**
 * 专业目录类型（优先级含义见 major_catalog.priority 与 docs/major-catalog.md）
 */
public enum CatalogType {

    /** 教育部通用目录（如普通高等学校本科专业目录） */
    MOE,

    /** 考试专用目录（某场考试发布的专业参考目录） */
    EXAM,

    /** 招录单位目录（用人单位/系统发布） */
    AGENCY,

    /** 自定义目录 */
    CUSTOM
}
