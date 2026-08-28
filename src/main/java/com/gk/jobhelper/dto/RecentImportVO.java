package com.gk.jobhelper.dto;

import java.time.LocalDateTime;

/**
 * 首页"最近分析"卡片数据（GET /api/import/recent）
 * 最近一次 Excel 导入记录 + 该批次岗位数 + 当前档案的匹配统计
 */
public class RecentImportVO {

    private Long importId;
    private String fileName;
    private String sheetName;
    /** Excel 数据总行数（不含表头） */
    private Integer totalRows;
    /** PREVIEWED / IMPORTED */
    private String status;
    private LocalDateTime createdAt;
    /** 该批次已导入的岗位数 */
    private Long jobCount;
    /** 当前档案在该批次的匹配统计；无档案或未执行匹配时各值为 0 */
    private MatchStats matchStats;

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getJobCount() {
        return jobCount;
    }

    public void setJobCount(Long jobCount) {
        this.jobCount = jobCount;
    }

    public MatchStats getMatchStats() {
        return matchStats;
    }

    public void setMatchStats(MatchStats matchStats) {
        this.matchStats = matchStats;
    }

    /** 匹配结果统计 */
    public static class MatchStats {

        private long total;
        private long match;
        private long uncertain;
        private long notMatch;

        public MatchStats() {
        }

        public MatchStats(long total, long match, long uncertain, long notMatch) {
            this.total = total;
            this.match = match;
            this.uncertain = uncertain;
            this.notMatch = notMatch;
        }

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
    }
}
