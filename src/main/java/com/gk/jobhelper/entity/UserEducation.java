package com.gk.jobhelper.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 可用于报考的一条教育经历。 */
public class UserEducation {
    private Long id;
    private Long profileId;
    private String educationLevel;
    private String degree;
    private String schoolName;
    private String majorName;
    private String majorCode;
    private LocalDate graduationDate;
    private Boolean highest;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getProfileId() { return profileId; } public void setProfileId(Long profileId) { this.profileId = profileId; }
    public String getEducationLevel() { return educationLevel; } public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getDegree() { return degree; } public void setDegree(String degree) { this.degree = degree; }
    public String getSchoolName() { return schoolName; } public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public String getMajorName() { return majorName; } public void setMajorName(String majorName) { this.majorName = majorName; }
    public String getMajorCode() { return majorCode; } public void setMajorCode(String majorCode) { this.majorCode = majorCode; }
    public LocalDate getGraduationDate() { return graduationDate; } public void setGraduationDate(LocalDate graduationDate) { this.graduationDate = graduationDate; }
    public Boolean getHighest() { return highest; } public void setHighest(Boolean highest) { this.highest = highest; }
    public Boolean getEnabled() { return enabled; } public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
