package com.gk.jobhelper.dto;
import java.math.BigDecimal;
public class HistoricalAnalysisVO {
 private Integer examYear;private BigDecimal minInterviewScore;private BigDecimal sampleMinScore;private BigDecimal sampleMaxScore;private BigDecimal sampleMedianScore;private BigDecimal sampleAverageScore;private String comparisonLevel;private int sampleCount;private Double percentile;private String relativeLevel;private String confidence;private String comparisonDescription;private boolean available;private boolean reliable;private boolean rankingEligible;
 public Integer getExamYear(){return examYear;}public void setExamYear(Integer v){examYear=v;}public BigDecimal getMinInterviewScore(){return minInterviewScore;}public void setMinInterviewScore(BigDecimal v){minInterviewScore=v;}
 public String getComparisonLevel(){return comparisonLevel;}public void setComparisonLevel(String v){comparisonLevel=v;}public int getSampleCount(){return sampleCount;}public void setSampleCount(int v){sampleCount=v;}
 public Double getPercentile(){return percentile;}public void setPercentile(Double v){percentile=v;}public String getRelativeLevel(){return relativeLevel;}public void setRelativeLevel(String v){relativeLevel=v;}
 public String getConfidence(){return confidence;}public void setConfidence(String v){confidence=v;}public String getComparisonDescription(){return comparisonDescription;}public void setComparisonDescription(String v){comparisonDescription=v;}
 public BigDecimal getSampleMinScore(){return sampleMinScore;}public void setSampleMinScore(BigDecimal v){sampleMinScore=v;}public BigDecimal getSampleMaxScore(){return sampleMaxScore;}public void setSampleMaxScore(BigDecimal v){sampleMaxScore=v;}public BigDecimal getSampleMedianScore(){return sampleMedianScore;}public void setSampleMedianScore(BigDecimal v){sampleMedianScore=v;}public BigDecimal getSampleAverageScore(){return sampleAverageScore;}public void setSampleAverageScore(BigDecimal v){sampleAverageScore=v;}
 public boolean isAvailable(){return available;}public void setAvailable(boolean v){available=v;}
 public boolean isReliable(){return reliable;}public void setReliable(boolean v){reliable=v;}
 public boolean isRankingEligible(){return rankingEligible;}public void setRankingEligible(boolean v){rankingEligible=v;}
}
