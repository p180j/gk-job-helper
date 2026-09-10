package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/** 招聘岗位资格判断中的单个条件证据。 */
public class RecruitmentMatchItem {
    private Long id, matchResultId, requirementId;
    private String requirementType, requirementLevel, result, positionRequirement, userEvidence, reason;
    private LocalDateTime createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getMatchResultId(){return matchResultId;} public void setMatchResultId(Long v){matchResultId=v;}
    public Long getRequirementId(){return requirementId;} public void setRequirementId(Long v){requirementId=v;}
    public String getRequirementType(){return requirementType;} public void setRequirementType(String v){requirementType=v;}
    public String getRequirementLevel(){return requirementLevel;} public void setRequirementLevel(String v){requirementLevel=v;}
    public String getResult(){return result;} public void setResult(String v){result=v;}
    public String getPositionRequirement(){return positionRequirement;} public void setPositionRequirement(String v){positionRequirement=v;}
    public String getUserEvidence(){return userEvidence;} public void setUserEvidence(String v){userEvidence=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
