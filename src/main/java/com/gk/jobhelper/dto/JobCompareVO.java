package com.gk.jobhelper.dto;

import com.gk.jobhelper.matcher.MatchItemResult;

import java.util.List;

/** 岗位横向对比行，复用岗位和已保存的资格匹配结果。 */
public class JobCompareVO {
    private Long jobId;
    private String region;
    private String departmentName;
    private String organizationName;
    private String positionName;
    private String positionCode;
    private Integer recruitCount;
    private String educationRequirement;
    private String majorRequirement;
    private String ageRequirement;
    private String politicalRequirement;
    private String workYearRequirement;
    private String freshGraduateRequirement;
    private String otherRestrictions;
    private String overallStatus;
    private List<MatchItemResult> matchItems;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public String getPositionCode() { return positionCode; }
    public void setPositionCode(String positionCode) { this.positionCode = positionCode; }
    public Integer getRecruitCount() { return recruitCount; }
    public void setRecruitCount(Integer recruitCount) { this.recruitCount = recruitCount; }
    public String getEducationRequirement() { return educationRequirement; }
    public void setEducationRequirement(String educationRequirement) { this.educationRequirement = educationRequirement; }
    public String getMajorRequirement() { return majorRequirement; }
    public void setMajorRequirement(String majorRequirement) { this.majorRequirement = majorRequirement; }
    public String getAgeRequirement() { return ageRequirement; }
    public void setAgeRequirement(String ageRequirement) { this.ageRequirement = ageRequirement; }
    public String getPoliticalRequirement() { return politicalRequirement; }
    public void setPoliticalRequirement(String politicalRequirement) { this.politicalRequirement = politicalRequirement; }
    public String getWorkYearRequirement() { return workYearRequirement; }
    public void setWorkYearRequirement(String workYearRequirement) { this.workYearRequirement = workYearRequirement; }
    public String getFreshGraduateRequirement() { return freshGraduateRequirement; }
    public void setFreshGraduateRequirement(String freshGraduateRequirement) { this.freshGraduateRequirement = freshGraduateRequirement; }
    public String getOtherRestrictions() { return otherRestrictions; }
    public void setOtherRestrictions(String otherRestrictions) { this.otherRestrictions = otherRestrictions; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public List<MatchItemResult> getMatchItems() { return matchItems; }
    public void setMatchItems(List<MatchItemResult> matchItems) { this.matchItems = matchItems; }
}
