package com.gk.jobhelper.dto;
public class RecruitmentDiscoveryResult {
 private int fetchedCount,newCount,duplicateCount,failedCount;
 public int getFetchedCount(){return fetchedCount;}public void setFetchedCount(int v){fetchedCount=v;}public int getNewCount(){return newCount;}public void setNewCount(int v){newCount=v;}public int getDuplicateCount(){return duplicateCount;}public void setDuplicateCount(int v){duplicateCount=v;}public int getFailedCount(){return failedCount;}public void setFailedCount(int v){failedCount=v;}
 public void merge(RecruitmentDiscoveryResult other){fetchedCount+=other.fetchedCount;newCount+=other.newCount;duplicateCount+=other.duplicateCount;failedCount+=other.failedCount;}
}
