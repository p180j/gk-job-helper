package com.gk.jobhelper.constant;

/**
 * 匹配条件类型（Iteration 3 支持 4 类，Iteration 4 新增专业匹配，后续迭代扩展户籍/应届生等）
 */
public enum ConditionType {

    /** 学历要求 */
    EDUCATION,

    /** 年龄要求 */
    AGE,

    /** 政治面貌要求 */
    POLITICAL,

    /** 基层工作年限要求 */
    WORK_EXPERIENCE,

    /** 专业要求（Iteration 4） */
    MAJOR
}
