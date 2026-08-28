package com.gk.jobhelper.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 个人档案创建/更新请求
 */
public class ProfileRequest {

    @Size(max = 64, message = "长度不能超过 64")
    private String name;

    @Size(max = 8, message = "长度不能超过 8")
    private String gender;

    /** 格式 yyyy-MM-dd */
    private LocalDate birthDate;

    @Size(max = 32, message = "长度不能超过 32")
    private String politicalStatus;

    @Size(max = 32, message = "长度不能超过 32")
    private String education;

    /** 学位(博士/硕士/学士) */
    @Size(max = 32, message = "长度不能超过 32")
    private String degree;

    @Size(max = 128, message = "长度不能超过 128")
    private String major;

    /** 专业代码(学科门类/专业类代码，逗号分隔) */
    @Size(max = 128, message = "长度不能超过 128")
    private String majorCode;

    /** 格式 yyyy-MM-dd */
    private LocalDate graduationDate;

    @Min(value = 0, message = "不能小于 0")
    @Max(value = 50, message = "不能大于 50")
    private Integer workYears;

    /** 应届生身份(是/否) */
    @Size(max = 32, message = "长度不能超过 32")
    private String freshGraduateStatus;

    /** 户籍 */
    @Size(max = 128, message = "长度不能超过 128")
    private String household;

    /** 生源地 */
    @Size(max = 128, message = "长度不能超过 128")
    private String studentOrigin;

    /** 服务基层项目类型 */
    @Size(max = 64, message = "长度不能超过 64")
    private String serviceProjectType;

    /** 退役军人(是/否) */
    @Size(max = 16, message = "长度不能超过 16")
    private String veteran;

    /** 持有证书(逗号分隔) */
    @Size(max = 500, message = "长度不能超过 500")
    private String certificates;

    @Size(max = 128, message = "长度不能超过 128")
    private String targetRegion;

    @Size(max = 1000, message = "长度不能超过 1000")
    private String notes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPoliticalStatus() {
        return politicalStatus;
    }

    public void setPoliticalStatus(String politicalStatus) {
        this.politicalStatus = politicalStatus;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getMajorCode() {
        return majorCode;
    }

    public void setMajorCode(String majorCode) {
        this.majorCode = majorCode;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public LocalDate getGraduationDate() {
        return graduationDate;
    }

    public void setGraduationDate(LocalDate graduationDate) {
        this.graduationDate = graduationDate;
    }

    public Integer getWorkYears() {
        return workYears;
    }

    public void setWorkYears(Integer workYears) {
        this.workYears = workYears;
    }

    public String getFreshGraduateStatus() {
        return freshGraduateStatus;
    }

    public void setFreshGraduateStatus(String freshGraduateStatus) {
        this.freshGraduateStatus = freshGraduateStatus;
    }

    public String getHousehold() {
        return household;
    }

    public void setHousehold(String household) {
        this.household = household;
    }

    public String getStudentOrigin() {
        return studentOrigin;
    }

    public void setStudentOrigin(String studentOrigin) {
        this.studentOrigin = studentOrigin;
    }

    public String getServiceProjectType() {
        return serviceProjectType;
    }

    public void setServiceProjectType(String serviceProjectType) {
        this.serviceProjectType = serviceProjectType;
    }

    public String getVeteran() {
        return veteran;
    }

    public void setVeteran(String veteran) {
        this.veteran = veteran;
    }

    public String getCertificates() {
        return certificates;
    }

    public void setCertificates(String certificates) {
        this.certificates = certificates;
    }

    public String getTargetRegion() {
        return targetRegion;
    }

    public void setTargetRegion(String targetRegion) {
        this.targetRegion = targetRegion;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
