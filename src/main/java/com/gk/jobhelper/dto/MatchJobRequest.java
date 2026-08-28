package com.gk.jobhelper.dto;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 单岗位匹配请求
 */
public class MatchJobRequest {

    @NotNull(message = "profileId 不能为空")
    private Long profileId;

    /** 匹配基准日期，可选；不传默认当前日期 */
    private LocalDate referenceDate;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }
}
