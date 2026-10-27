package com.gk.jobhelper.dto;
import java.util.ArrayList; import java.util.List;
public class JobFeatureVO {
 private Long positionId; private Integer examSubjectCount; private List<String> examSubjects=new ArrayList<>();
 private String examSubjectGroup; private String examSubjectStatus; private String rawExamSubjectText;
 private String majorRestrictionType; private List<String> majorDomains=new ArrayList<>(),majorSimilarityKeys=new ArrayList<>(); private Integer majorScopeCount;
 private String majorAnalysisStatus; private String organizationLevel;
 public Long getPositionId(){return positionId;} public void setPositionId(Long v){positionId=v;}
 public Integer getExamSubjectCount(){return examSubjectCount;} public void setExamSubjectCount(Integer v){examSubjectCount=v;}
 public List<String> getExamSubjects(){return examSubjects;} public void setExamSubjects(List<String> v){examSubjects=v;}
 public String getExamSubjectGroup(){return examSubjectGroup;} public void setExamSubjectGroup(String v){examSubjectGroup=v;}
 public String getExamSubjectStatus(){return examSubjectStatus;} public void setExamSubjectStatus(String v){examSubjectStatus=v;}
 public String getRawExamSubjectText(){return rawExamSubjectText;} public void setRawExamSubjectText(String v){rawExamSubjectText=v;}
 public String getMajorRestrictionType(){return majorRestrictionType;} public void setMajorRestrictionType(String v){majorRestrictionType=v;}
 public List<String> getMajorDomains(){return majorDomains;} public void setMajorDomains(List<String> v){majorDomains=v;}
 public List<String> getMajorSimilarityKeys(){return majorSimilarityKeys;} public void setMajorSimilarityKeys(List<String> v){majorSimilarityKeys=v;}
 public Integer getMajorScopeCount(){return majorScopeCount;} public void setMajorScopeCount(Integer v){majorScopeCount=v;}
 public String getMajorAnalysisStatus(){return majorAnalysisStatus;} public void setMajorAnalysisStatus(String v){majorAnalysisStatus=v;}
 public String getOrganizationLevel(){return organizationLevel;} public void setOrganizationLevel(String v){organizationLevel=v;}
 private String effectiveMajorRequirement,remarkConditions,workLocationName,workLocationCode,workLocationStatus,workLocationSource,workLocationEvidence,workLocationType;
 public String getEffectiveMajorRequirement(){return effectiveMajorRequirement;} public void setEffectiveMajorRequirement(String v){effectiveMajorRequirement=v;}
 public String getRemarkConditions(){return remarkConditions;} public void setRemarkConditions(String v){remarkConditions=v;}
 public String getWorkLocationName(){return workLocationName;} public void setWorkLocationName(String v){workLocationName=v;}
 public String getWorkLocationCode(){return workLocationCode;} public void setWorkLocationCode(String v){workLocationCode=v;}
 public String getWorkLocationStatus(){return workLocationStatus;} public void setWorkLocationStatus(String v){workLocationStatus=v;}
 public String getWorkLocationSource(){return workLocationSource;} public void setWorkLocationSource(String v){workLocationSource=v;}
 public String getWorkLocationEvidence(){return workLocationEvidence;} public void setWorkLocationEvidence(String v){workLocationEvidence=v;}
 public String getWorkLocationType(){return workLocationType;} public void setWorkLocationType(String v){workLocationType=v;}
}
