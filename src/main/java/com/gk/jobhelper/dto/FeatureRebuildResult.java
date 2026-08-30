package com.gk.jobhelper.dto;
public class FeatureRebuildResult {
 private Long importId; private int total; private int success; private int unknownSubjects; private int uncertainMajors;
 public Long getImportId(){return importId;} public void setImportId(Long v){importId=v;}
 public int getTotal(){return total;} public void setTotal(int v){total=v;}
 public int getSuccess(){return success;} public void setSuccess(int v){success=v;}
 public int getUnknownSubjects(){return unknownSubjects;} public void setUnknownSubjects(int v){unknownSubjects=v;}
 public int getUncertainMajors(){return uncertainMajors;} public void setUncertainMajors(int v){uncertainMajors=v;}
}
