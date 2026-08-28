package com.gk.jobhelper.dto;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 批量匹配请求：对指定导入批次下所有岗位执行匹配
 */
public class MatchExecuteRequest {

    @NotNull(message = "profileId 不能为空")
    private Long profileId;

    @NotNull(message = "importId 不能为空")
    private Long importId;

    /** 匹配基准日期，可选；不传默认当前日期 */
    private LocalDate referenceDate;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }
}
