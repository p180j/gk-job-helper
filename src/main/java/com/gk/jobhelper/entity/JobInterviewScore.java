package com.gk.jobhelper.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class JobInterviewScore {
    private Long id; private Integer examYear; private String positionCode;
    private String departmentName; private String positionName; private BigDecimal minInterviewScore;
    private Integer interviewCandidateCount; private String sourceFileName;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Integer getExamYear(){return examYear;} public void setExamYear(Integer v){examYear=v;}
    public String getPositionCode(){return positionCode;} public void setPositionCode(String v){positionCode=v;}
    public String getDepartmentName(){return departmentName;} public void setDepartmentName(String v){departmentName=v;}
    public String getPositionName(){return positionName;} public void setPositionName(String v){positionName=v;}
    public BigDecimal getMinInterviewScore(){return minInterviewScore;} public void setMinInterviewScore(BigDecimal v){minInterviewScore=v;}
    public Integer getInterviewCandidateCount(){return interviewCandidateCount;} public void setInterviewCandidateCount(Integer v){interviewCandidateCount=v;}
    public String getSourceFileName(){return sourceFileName;} public void setSourceFileName(String v){sourceFileName=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
