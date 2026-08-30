package com.gk.jobhelper.dto;
import java.util.ArrayList; import java.util.List;
public class JobFeatureVO {
 private Long positionId; private Integer examSubjectCount; private List<String> examSubjects=new ArrayList<>();
 private String examSubjectGroup; private String examSubjectStatus; private String rawExamSubjectText;
 private String majorRestrictionType; private List<String> majorDomains=new ArrayList<>(); private Integer majorScopeCount;
 private String majorAnalysisStatus; private String organizationLevel;
 public Long getPositionId(){return positionId;} public void setPositionId(Long v){positionId=v;}
 public Integer getExamSubjectCount(){return examSubjectCount;} public void setExamSubjectCount(Integer v){examSubjectCount=v;}
 public List<String> getExamSubjects(){return examSubjects;} public void setExamSubjects(List<String> v){examSubjects=v;}
 public String getExamSubjectGroup(){return examSubjectGroup;} public void setExamSubjectGroup(String v){examSubjectGroup=v;}
 public String getExamSubjectStatus(){return examSubjectStatus;} public void setExamSubjectStatus(String v){examSubjectStatus=v;}
 public String getRawExamSubjectText(){return rawExamSubjectText;} public void setRawExamSubjectText(String v){rawExamSubjectText=v;}
 public String getMajorRestrictionType(){return majorRestrictionType;} public void setMajorRestrictionType(String v){majorRestrictionType=v;}
 public List<String> getMajorDomains(){return majorDomains;} public void setMajorDomains(List<String> v){majorDomains=v;}
 public Integer getMajorScopeCount(){return majorScopeCount;} public void setMajorScopeCount(Integer v){majorScopeCount=v;}
 public String getMajorAnalysisStatus(){return majorAnalysisStatus;} public void setMajorAnalysisStatus(String v){majorAnalysisStatus=v;}
 public String getOrganizationLevel(){return organizationLevel;} public void setOrganizationLevel(String v){organizationLevel=v;}
}
