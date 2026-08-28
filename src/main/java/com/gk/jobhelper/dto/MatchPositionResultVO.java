package com.gk.jobhelper.dto;

import java.time.LocalDate;

/**
 * 匹配结果分页查询返回行：岗位基本信息 + 综合匹配结果
 */
public class MatchPositionResultVO {

    private Long jobId;
    private String positionName;
    private String positionCode;
    private String departmentName;
    private String organizationName;
    private String province;
    private String city;
    private Integer recruitCount;
    private String educationRequirement;
    private String majorRequirement;
    private String matchResult;
    private LocalDate referenceDate;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getRecruitCount() {
        return recruitCount;
    }

    public void setRecruitCount(Integer recruitCount) {
        this.recruitCount = recruitCount;
    }

    public String getEducationRequirement() {
        return educationRequirement;
    }

    public void setEducationRequirement(String educationRequirement) {
        this.educationRequirement = educationRequirement;
    }

    public String getMajorRequirement() {
        return majorRequirement;
    }

    public void setMajorRequirement(String majorRequirement) {
        this.majorRequirement = majorRequirement;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }
}
