package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量匹配执行结果统计
 */
public class MatchSummaryVO {

    /** 参与匹配的岗位总数 */
    private long total;
    private long match;
    private long uncertain;
    private long notMatch;
    /** 匹配执行异常的岗位数 */
    private long failedCount;
    /** 失败明细（数量有限，避免响应过大） */
    private List<FailedItem> failedItems = new ArrayList<>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getMatch() {
        return match;
    }

    public void setMatch(long match) {
        this.match = match;
    }

    public long getUncertain() {
        return uncertain;
    }

    public void setUncertain(long uncertain) {
        this.uncertain = uncertain;
    }

    public long getNotMatch() {
        return notMatch;
    }

    public void setNotMatch(long notMatch) {
        this.notMatch = notMatch;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public List<FailedItem> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<FailedItem> failedItems) {
        this.failedItems = failedItems;
    }

    /** 单个岗位匹配失败明细 */
    public static class FailedItem {

        private Long jobId;
        private String reason;

        public FailedItem() {
        }

        public FailedItem(Long jobId, String reason) {
            this.jobId = jobId;
            this.reason = reason;
        }

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
