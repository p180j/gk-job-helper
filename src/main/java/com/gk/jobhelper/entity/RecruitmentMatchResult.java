package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/** 一个档案对一个招聘岗位保留一条最新资格判断。 */
public class RecruitmentMatchResult {
    private Long id, profileId, positionId;
    private String result, summary, uncertainReason;
    private Integer matchedCount, uncertainCount, notMatchedCount;
    private LocalDateTime createdAt, updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getProfileId(){return profileId;} public void setProfileId(Long v){profileId=v;}
    public Long getPositionId(){return positionId;} public void setPositionId(Long v){positionId=v;}
    public String getResult(){return result;} public void setResult(String v){result=v;}
    public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
    public String getUncertainReason(){return uncertainReason;} public void setUncertainReason(String v){uncertainReason=v;}
    public Integer getMatchedCount(){return matchedCount;} public void setMatchedCount(Integer v){matchedCount=v;}
    public Integer getUncertainCount(){return uncertainCount;} public void setUncertainCount(Integer v){uncertainCount=v;}
    public Integer getNotMatchedCount(){return notMatchedCount;} public void setNotMatchedCount(Integer v){notMatchedCount=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
