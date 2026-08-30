package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

public class JobPreference {
    private Long id; private Long profileId; private String preferredRegions; private String acceptedOrgLevels;
    private String excludedOrgLevels; private String preferredSubjectGroups;
    private Boolean acceptExtraSubjects; private Boolean preferMoreRecruits;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getProfileId(){return profileId;} public void setProfileId(Long v){profileId=v;}
    public String getPreferredRegions(){return preferredRegions;} public void setPreferredRegions(String v){preferredRegions=v;}
    public String getAcceptedOrgLevels(){return acceptedOrgLevels;} public void setAcceptedOrgLevels(String v){acceptedOrgLevels=v;}
    public String getExcludedOrgLevels(){return excludedOrgLevels;} public void setExcludedOrgLevels(String v){excludedOrgLevels=v;}
    public String getPreferredSubjectGroups(){return preferredSubjectGroups;} public void setPreferredSubjectGroups(String v){preferredSubjectGroups=v;}
    public Boolean getAcceptExtraSubjects(){return acceptExtraSubjects;} public void setAcceptExtraSubjects(Boolean v){acceptExtraSubjects=v;}
    public Boolean getPreferMoreRecruits(){return preferMoreRecruits;} public void setPreferMoreRecruits(Boolean v){preferMoreRecruits=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
