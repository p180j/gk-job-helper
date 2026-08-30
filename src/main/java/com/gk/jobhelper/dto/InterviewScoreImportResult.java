package com.gk.jobhelper.dto;
import java.util.ArrayList; import java.util.List;
public class InterviewScoreImportResult {
 private Long importId; private Integer examYear; private int rawRowCount; private int aggregatedPositionCount; private int insertedCount; private int updatedCount; private int linkedPositionCount; private int unlinkedPositionCount; private int invalidRowCount; private List<String> invalidRows=new ArrayList<>();
 public Long getImportId(){return importId;}public void setImportId(Long v){importId=v;}
 public Integer getExamYear(){return examYear;}public void setExamYear(Integer v){examYear=v;}
 public int getRawRowCount(){return rawRowCount;}public void setRawRowCount(int v){rawRowCount=v;}
 public int getAggregatedPositionCount(){return aggregatedPositionCount;}public void setAggregatedPositionCount(int v){aggregatedPositionCount=v;}
 public int getInsertedCount(){return insertedCount;}public void setInsertedCount(int v){insertedCount=v;}
 public int getUpdatedCount(){return updatedCount;}public void setUpdatedCount(int v){updatedCount=v;}
 public int getLinkedPositionCount(){return linkedPositionCount;}public void setLinkedPositionCount(int v){linkedPositionCount=v;}
 public int getUnlinkedPositionCount(){return unlinkedPositionCount;}public void setUnlinkedPositionCount(int v){unlinkedPositionCount=v;}
 public int getInvalidRowCount(){return invalidRowCount;}public void setInvalidRowCount(int v){invalidRowCount=v;}
 public List<String> getInvalidRows(){return invalidRows;}public void setInvalidRows(List<String> v){invalidRows=v;}
}
