package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/**
 * 岗位匹配条件明细：每个条件类型（EDUCATION/AGE/POLITICAL/WORK_EXPERIENCE/MAJOR/REMARK）一条记录
 */
public class JobMatchItem {

    private Long id;
    private Long jobMatchId;
    private Long jobPositionId;
    /** 条件类型，见 ConditionType */
    private String conditionType;
    /** MATCH / UNCERTAIN / NOT_MATCH */
    private String matchResult;
    private String userValue;
    private String requirementValue;
    private String reason;
    /** 匹配证据 JSON（专业目录来源等结构化信息，Iteration 4） */
    private String evidence;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobMatchId() {
        return jobMatchId;
    }

    public void setJobMatchId(Long jobMatchId) {
        this.jobMatchId = jobMatchId;
    }

    public Long getJobPositionId() {
        return jobPositionId;
    }

    public void setJobPositionId(Long jobPositionId) {
        this.jobPositionId = jobPositionId;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public String getRequirementValue() {
        return requirementValue;
    }

    public void setRequirementValue(String requirementValue) {
        this.requirementValue = requirementValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
