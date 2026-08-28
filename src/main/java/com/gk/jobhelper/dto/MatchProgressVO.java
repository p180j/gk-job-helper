package com.gk.jobhelper.dto;

/** 首页轮询用的后台匹配进度。 */
public class MatchProgressVO extends MatchSummaryVO {
    private String status;
    private String errorMessage;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public long getProcessed() { return getMatch() + getUncertain() + getNotMatch() + getFailedCount(); }
}
