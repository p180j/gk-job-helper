package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/** 用户确认后的招聘职业画像；经历、技能和证书以 JSON 文本保存。 */
public class CareerProfile {
    private Long id;
    private Long profileId;
    private String currentPosition;
    private String totalWorkYears;
    private String careerDirections;
    private String industries;
    private String educationExperiences;
    private String workExperiences;
    private String projectExperiences;
    private String skills;
    private String certificates;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    public String getTotalWorkYears() { return totalWorkYears; }
    public void setTotalWorkYears(String totalWorkYears) { this.totalWorkYears = totalWorkYears; }
    public String getCareerDirections() { return careerDirections; }
    public void setCareerDirections(String careerDirections) { this.careerDirections = careerDirections; }
    public String getIndustries() { return industries; }
    public void setIndustries(String industries) { this.industries = industries; }
    public String getEducationExperiences() { return educationExperiences; }
    public void setEducationExperiences(String educationExperiences) { this.educationExperiences = educationExperiences; }
    public String getWorkExperiences() { return workExperiences; }
    public void setWorkExperiences(String workExperiences) { this.workExperiences = workExperiences; }
    public String getProjectExperiences() { return projectExperiences; }
    public void setProjectExperiences(String projectExperiences) { this.projectExperiences = projectExperiences; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getCertificates() { return certificates; }
    public void setCertificates(String certificates) { this.certificates = certificates; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
