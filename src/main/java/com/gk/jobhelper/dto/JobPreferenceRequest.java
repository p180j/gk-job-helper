package com.gk.jobhelper.dto;
import java.util.ArrayList;import java.util.List;
public class JobPreferenceRequest {
 private List<String> preferredRegions=new ArrayList<>(),acceptedOrgLevels=new ArrayList<>(),excludedOrgLevels=new ArrayList<>(),preferredSubjectGroups=new ArrayList<>();private Boolean acceptExtraSubjects=true,preferMoreRecruits=true;
 public List<String> getPreferredRegions(){return preferredRegions;}public void setPreferredRegions(List<String>v){preferredRegions=v;}public List<String> getAcceptedOrgLevels(){return acceptedOrgLevels;}public void setAcceptedOrgLevels(List<String>v){acceptedOrgLevels=v;}
 public List<String> getExcludedOrgLevels(){return excludedOrgLevels;}public void setExcludedOrgLevels(List<String>v){excludedOrgLevels=v;}public List<String> getPreferredSubjectGroups(){return preferredSubjectGroups;}public void setPreferredSubjectGroups(List<String>v){preferredSubjectGroups=v;}
 public Boolean getAcceptExtraSubjects(){return acceptExtraSubjects;}public void setAcceptExtraSubjects(Boolean v){acceptExtraSubjects=v;}public Boolean getPreferMoreRecruits(){return preferMoreRecruits;}public void setPreferMoreRecruits(Boolean v){preferMoreRecruits=v;}
}
