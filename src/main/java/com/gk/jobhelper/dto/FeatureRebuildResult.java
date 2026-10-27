package com.gk.jobhelper.dto;
public class FeatureRebuildResult {
 private Long importId; private int total; private int success; private int unknownSubjects; private int uncertainMajors;
 private int changed,confirmedLocations,inferredLocations,multiSiteLocations,unknownLocations;
 private boolean preview;
 public Long getImportId(){return importId;} public void setImportId(Long v){importId=v;}
 public int getTotal(){return total;} public void setTotal(int v){total=v;}
 public int getSuccess(){return success;} public void setSuccess(int v){success=v;}
 public int getUnknownSubjects(){return unknownSubjects;} public void setUnknownSubjects(int v){unknownSubjects=v;}
 public int getUncertainMajors(){return uncertainMajors;} public void setUncertainMajors(int v){uncertainMajors=v;}
 public int getChanged(){return changed;} public void setChanged(int v){changed=v;}
 public int getConfirmedLocations(){return confirmedLocations;} public void setConfirmedLocations(int v){confirmedLocations=v;}
 public int getInferredLocations(){return inferredLocations;} public void setInferredLocations(int v){inferredLocations=v;}
 public int getMultiSiteLocations(){return multiSiteLocations;} public void setMultiSiteLocations(int v){multiSiteLocations=v;}
 public int getUnknownLocations(){return unknownLocations;} public void setUnknownLocations(int v){unknownLocations=v;}
 public boolean isPreview(){return preview;} public void setPreview(boolean v){preview=v;}
}
