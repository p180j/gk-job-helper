package com.gk.jobhelper.dto;

/**
 * 匹配结果分页查询条件（mapper 查询参数）
 */
public class MatchResultQuery {

    private Long profileId;
    private Long importFileId;
    /** MATCH / UNCERTAIN / NOT_MATCH，null 表示不过滤 */
    private String matchResult;
    private int offset;
    private int size;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
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

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
