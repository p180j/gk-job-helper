package com.gk.jobhelper.constant;

/**
 * 专业别名类型。只允许来源明确或人工维护的别名，禁止自动相似推断。
 */
public enum AliasType {

    /** 官方曾用名 / 目录内官方别称 */
    OFFICIAL,

    /** 人工维护别名 */
    MANUAL
}
