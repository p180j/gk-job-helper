package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/**
 * 标准岗位模型（Iteration 2 完整字段）
 */
public class JobPosition {

    private Long id;
    private Long examId;
    private Long importFileId;
    private String departmentName;
    private String organizationName;
    private String positionName;
    private String positionCode;
    private String province;
    private String city;
    private String district;
    private Integer recruitCount;
    private String educationRequirement;
    private String degreeRequirement;
    private String majorRequirement;
    private String majorCodes;
    private String ageRequirement;
    private String politicalRequirement;
    private String workYearRequirement;
    private String freshGraduateRequirement;
    private String householdRequirement;
    private String serviceProjectRequirement;
    private String certificateRequirement;
    private String genderRequirement;
    private String positionDescription;
    private String remark;
    private String sourceSheet;
    private Integer sourceRow;
    private String rawData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getImportFileId() {
        return importFileId;
    }

    public void setImportFileId(Long importFileId) {
        this.importFileId = importFileId;
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

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
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

    public String getDegreeRequirement() {
        return degreeRequirement;
    }

    public void setDegreeRequirement(String degreeRequirement) {
        this.degreeRequirement = degreeRequirement;
    }

    public String getMajorRequirement() {
        return majorRequirement;
    }

    public void setMajorRequirement(String majorRequirement) {
        this.majorRequirement = majorRequirement;
    }

    public String getMajorCodes() {
        return majorCodes;
    }

    public void setMajorCodes(String majorCodes) {
        this.majorCodes = majorCodes;
    }

    public String getAgeRequirement() {
        return ageRequirement;
    }

    public void setAgeRequirement(String ageRequirement) {
        this.ageRequirement = ageRequirement;
    }

    public String getPoliticalRequirement() {
        return politicalRequirement;
    }

    public void setPoliticalRequirement(String politicalRequirement) {
        this.politicalRequirement = politicalRequirement;
    }

    public String getWorkYearRequirement() {
        return workYearRequirement;
    }

    public void setWorkYearRequirement(String workYearRequirement) {
        this.workYearRequirement = workYearRequirement;
    }

    public String getFreshGraduateRequirement() {
        return freshGraduateRequirement;
    }

    public void setFreshGraduateRequirement(String freshGraduateRequirement) {
        this.freshGraduateRequirement = freshGraduateRequirement;
    }

    public String getHouseholdRequirement() {
        return householdRequirement;
    }

    public void setHouseholdRequirement(String householdRequirement) {
        this.householdRequirement = householdRequirement;
    }

    public String getServiceProjectRequirement() {
        return serviceProjectRequirement;
    }

    public void setServiceProjectRequirement(String serviceProjectRequirement) {
        this.serviceProjectRequirement = serviceProjectRequirement;
    }

    public String getCertificateRequirement() {
        return certificateRequirement;
    }

    public void setCertificateRequirement(String certificateRequirement) {
        this.certificateRequirement = certificateRequirement;
    }

    public String getGenderRequirement() {
        return genderRequirement;
    }

    public void setGenderRequirement(String genderRequirement) {
        this.genderRequirement = genderRequirement;
    }

    public String getPositionDescription() {
        return positionDescription;
    }

    public void setPositionDescription(String positionDescription) {
        this.positionDescription = positionDescription;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSourceSheet() {
        return sourceSheet;
    }

    public void setSourceSheet(String sourceSheet) {
        this.sourceSheet = sourceSheet;
    }

    public Integer getSourceRow() {
        return sourceRow;
    }

    public void setSourceRow(Integer sourceRow) {
        this.sourceRow = sourceRow;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
