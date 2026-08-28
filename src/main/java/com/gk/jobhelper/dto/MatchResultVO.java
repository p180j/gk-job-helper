package com.gk.jobhelper.dto;

import com.gk.jobhelper.matcher.MatchItemResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 单岗位匹配结果 / 匹配详情返回
 */
public class MatchResultVO {

    private Long jobId;
    private Long profileId;
    /** 综合匹配结果 MATCH / UNCERTAIN / NOT_MATCH */
    private String result;
    private LocalDate referenceDate;
    /** 各条件明细 */
    private List<MatchItemResult> items;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    public List<MatchItemResult> getItems() {
        return items;
    }

    public void setItems(List<MatchItemResult> items) {
        this.items = items;
    }
}
