package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;

/**
 * 单个条件的匹配判定结果（同时作为接口返回元素）。
 * reason 必须为用户可直接阅读的中文说明。
 * evidence 为可选的结构化匹配证据（专业目录来源等，Iteration 4 起）。
 */
public class MatchItemResult {

    private final ConditionType conditionType;
    private final MatchResult result;
    /** 用户档案对应原始值（如"本科"、年龄数值"32"） */
    private final String userValue;
    /** 岗位要求原始值（如"本科及以上"） */
    private final String requirementValue;
    /** 中文可读判定原因 */
    private final String reason;
    /** 结构化匹配证据（可空） */
    private final MatchEvidence evidence;

    public MatchItemResult(ConditionType conditionType, MatchResult result,
                           String userValue, String requirementValue, String reason) {
        this(conditionType, result, userValue, requirementValue, reason, null);
    }

    public MatchItemResult(ConditionType conditionType, MatchResult result,
                           String userValue, String requirementValue, String reason,
                           MatchEvidence evidence) {
        this.conditionType = conditionType;
        this.result = result;
        this.userValue = userValue;
        this.requirementValue = requirementValue;
        this.reason = reason;
        this.evidence = evidence;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public MatchResult getResult() {
        return result;
    }

    public String getUserValue() {
        return userValue;
    }

    public String getRequirementValue() {
        return requirementValue;
    }

    public String getReason() {
        return reason;
    }

    public MatchEvidence getEvidence() {
        return evidence;
    }
}
