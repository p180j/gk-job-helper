package com.gk.jobhelper.constant;

/**
 * 专业目录节点层级。
 * 注意：本科"专业类"与研究生"一级学科"不是同一层级概念，禁止混用，
 * 层级关系全部由 major_catalog_item.parent_id 表达。
 */
public enum CatalogItemLevel {

    /** 学科门类（如 08 工学 / 03 法学） */
    CATEGORY,

    /** 本科专业类（如 0809 计算机类） */
    CLASS,

    /** 本科专业（如 080902 软件工程） */
    MAJOR,

    /** 研究生一级学科（如 0812 计算机科学与技术） */
    DISCIPLINE,

    /** 研究生专业学位类别（如 0854 电子信息） */
    FIELD,

    /** 其他 */
    OTHER
}
