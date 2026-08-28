package com.gk.jobhelper.constant;

/**
 * 匹配结果语义（禁止为提高匹配率将不确定条件判为 MATCH）：
 * MATCH      - 明确满足岗位条件
 * NOT_MATCH  - 明确不满足岗位条件
 * UNCERTAIN  - 信息不足、要求模糊或规则无法可靠判断
 */
public enum MatchResult {
    MATCH,
    UNCERTAIN,
    NOT_MATCH
}
