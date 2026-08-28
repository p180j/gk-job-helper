package com.gk.jobhelper.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 岗位匹配结果：一个档案 + 一个岗位保留最新匹配结果（重复匹配覆盖更新）
 */
public class JobMatch {

    private Long id;
    private Long profileId;
    private Long jobPositionId;
    private Long importFileId;
    /** MATCH / UNCERTAIN / NOT_MATCH */
    private String matchResult;
    /** 匹配基准日期 */
    private LocalDate referenceDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getJobPositionId() {
        return jobPositionId;
    }

    public void setJobPositionId(Long jobPositionId) {
        this.jobPositionId = jobPositionId;
    }

    public Long getImportFileId() {
        return importFileId;
    }

    public void setImportFileId(Long importFileId) {
        this.importFileId = importFileId;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
