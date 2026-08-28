package com.gk.jobhelper.dto;

/**
 * 匹配结果分组统计行（job_match group by match_result）
 */
public class MatchResultStatRow {

    private String matchResult;
    private long cnt;

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public long getCnt() {
        return cnt;
    }

    public void setCnt(long cnt) {
        this.cnt = cnt;
    }
}
