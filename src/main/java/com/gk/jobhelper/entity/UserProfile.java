package com.gk.jobhelper.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户个人档案
 */
public class UserProfile {

    private Long id;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private String politicalStatus;
    private String education;
    /** 学位 */
    private String degree;
    private String major;
    /** 专业代码 */
    private String majorCode;
    /** 专业对应学历层次(本科/研究生/专科)；空则按 education 推断。第一阶段仍只用最高学历对应专业 */
    private String majorEducationLevel;
    private LocalDate graduationDate;
    private Integer workYears;
    /** 应届生身份(是/否) */
    private String freshGraduateStatus;
    /** 户籍 */
    private String household;
    /** 生源地 */
    private String studentOrigin;
    /** 服务基层项目类型 */
    private String serviceProjectType;
    /** 退役军人(是/否) */
    private String veteran;
    /** 持有证书(逗号分隔) */
    private String certificates;
    private String targetRegion;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getMajorEducationLevel() {
        return majorEducationLevel;
    }

    public void setMajorEducationLevel(String majorEducationLevel) {
        this.majorEducationLevel = majorEducationLevel;
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
