package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

public class JobPositionFeature {
    private Long positionId; private Integer examSubjectCount; private String examSubjects;
    private String examSubjectGroup; private String examSubjectStatus; private String rawExamSubjectText;
    private String majorRestrictionType; private String majorDomains; private String majorSimilarityKeys; private Integer majorScopeCount;
    private String majorAnalysisStatus; private String organizationLevel; private String organizationLevelStatus;
    private String effectiveMajorRequirement, remarkConditions;
    private String workLocationName, workLocationCode, workLocationStatus, workLocationSource;
    private String workLocationEvidence, workLocationType;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getPositionId(){return positionId;} public void setPositionId(Long v){positionId=v;}
    public Integer getExamSubjectCount(){return examSubjectCount;} public void setExamSubjectCount(Integer v){examSubjectCount=v;}
    public String getExamSubjects(){return examSubjects;} public void setExamSubjects(String v){examSubjects=v;}
    public String getExamSubjectGroup(){return examSubjectGroup;} public void setExamSubjectGroup(String v){examSubjectGroup=v;}
    public String getExamSubjectStatus(){return examSubjectStatus;} public void setExamSubjectStatus(String v){examSubjectStatus=v;}
    public String getRawExamSubjectText(){return rawExamSubjectText;} public void setRawExamSubjectText(String v){rawExamSubjectText=v;}
    public String getMajorRestrictionType(){return majorRestrictionType;} public void setMajorRestrictionType(String v){majorRestrictionType=v;}
    public String getMajorDomains(){return majorDomains;} public void setMajorDomains(String v){majorDomains=v;}
    public String getMajorSimilarityKeys(){return majorSimilarityKeys;} public void setMajorSimilarityKeys(String v){majorSimilarityKeys=v;}
    public Integer getMajorScopeCount(){return majorScopeCount;} public void setMajorScopeCount(Integer v){majorScopeCount=v;}
    public String getMajorAnalysisStatus(){return majorAnalysisStatus;} public void setMajorAnalysisStatus(String v){majorAnalysisStatus=v;}
    public String getOrganizationLevel(){return organizationLevel;} public void setOrganizationLevel(String v){organizationLevel=v;}
    public String getOrganizationLevelStatus(){return organizationLevelStatus;} public void setOrganizationLevelStatus(String v){organizationLevelStatus=v;}
    public String getEffectiveMajorRequirement(){return effectiveMajorRequirement;} public void setEffectiveMajorRequirement(String v){effectiveMajorRequirement=v;}
    public String getRemarkConditions(){return remarkConditions;} public void setRemarkConditions(String v){remarkConditions=v;}
    public String getWorkLocationName(){return workLocationName;} public void setWorkLocationName(String v){workLocationName=v;}
    public String getWorkLocationCode(){return workLocationCode;} public void setWorkLocationCode(String v){workLocationCode=v;}
    public String getWorkLocationStatus(){return workLocationStatus;} public void setWorkLocationStatus(String v){workLocationStatus=v;}
    public String getWorkLocationSource(){return workLocationSource;} public void setWorkLocationSource(String v){workLocationSource=v;}
    public String getWorkLocationEvidence(){return workLocationEvidence;} public void setWorkLocationEvidence(String v){workLocationEvidence=v;}
    public String getWorkLocationType(){return workLocationType;} public void setWorkLocationType(String v){workLocationType=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
