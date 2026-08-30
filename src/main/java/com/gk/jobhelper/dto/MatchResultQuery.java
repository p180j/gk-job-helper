package com.gk.jobhelper.dto;

/**
 * 匹配结果分页查询条件（mapper 查询参数）
 */
public class MatchResultQuery {

    private Long profileId;
    private Long importFileId;
    /** MATCH / UNCERTAIN / NOT_MATCH，null 表示不过滤 */
    private String matchResult;
    private String region;
    private String organizationKeyword;
    private String positionKeyword;
    private Integer recruitCountMin;
    private Integer recruitCountMax;
    private String educationKeyword;
    private String majorKeyword;
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

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getOrganizationKeyword() { return organizationKeyword; }
    public void setOrganizationKeyword(String organizationKeyword) { this.organizationKeyword = organizationKeyword; }
    public String getPositionKeyword() { return positionKeyword; }
    public void setPositionKeyword(String positionKeyword) { this.positionKeyword = positionKeyword; }
    public Integer getRecruitCountMin() { return recruitCountMin; }
    public void setRecruitCountMin(Integer recruitCountMin) { this.recruitCountMin = recruitCountMin; }
    public Integer getRecruitCountMax() { return recruitCountMax; }
    public void setRecruitCountMax(Integer recruitCountMax) { this.recruitCountMax = recruitCountMax; }
    public String getEducationKeyword() { return educationKeyword; }
    public void setEducationKeyword(String educationKeyword) { this.educationKeyword = educationKeyword; }
    public String getMajorKeyword() { return majorKeyword; }
    public void setMajorKeyword(String majorKeyword) { this.majorKeyword = majorKeyword; }

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
