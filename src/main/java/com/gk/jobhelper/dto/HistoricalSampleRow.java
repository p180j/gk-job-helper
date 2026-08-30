package com.gk.jobhelper.dto;
import java.math.BigDecimal;
public class HistoricalSampleRow {
 private Long positionId;private Integer examYear;private String positionCode;private Integer recruitCount;private BigDecimal minInterviewScore;private String examSubjectGroup;private String majorRestrictionType;private String majorDomains;
 public Long getPositionId(){return positionId;}public void setPositionId(Long v){positionId=v;}public String getPositionCode(){return positionCode;}public void setPositionCode(String v){positionCode=v;}
 public Integer getExamYear(){return examYear;}public void setExamYear(Integer v){examYear=v;}
 public Integer getRecruitCount(){return recruitCount;}public void setRecruitCount(Integer v){recruitCount=v;}public BigDecimal getMinInterviewScore(){return minInterviewScore;}public void setMinInterviewScore(BigDecimal v){minInterviewScore=v;}
 public String getExamSubjectGroup(){return examSubjectGroup;}public void setExamSubjectGroup(String v){examSubjectGroup=v;}public String getMajorRestrictionType(){return majorRestrictionType;}public void setMajorRestrictionType(String v){majorRestrictionType=v;}public String getMajorDomains(){return majorDomains;}public void setMajorDomains(String v){majorDomains=v;}
}
