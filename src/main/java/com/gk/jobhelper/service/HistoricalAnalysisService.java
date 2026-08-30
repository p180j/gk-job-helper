package com.gk.jobhelper.service;

import com.fasterxml.jackson.core.type.TypeReference;import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.dto.*;import com.gk.jobhelper.entity.*;import com.gk.jobhelper.mapper.*;import org.springframework.stereotype.Service;
import java.math.BigDecimal;import java.math.RoundingMode;import java.util.*;

@Service
public class HistoricalAnalysisService {
 private final JobPositionMapper positionMapper;private final JobPositionFeatureMapper featureMapper;private final JobInterviewScoreMapper scoreMapper;private final ObjectMapper objectMapper;
 public HistoricalAnalysisService(JobPositionMapper p,JobPositionFeatureMapper f,JobInterviewScoreMapper s,ObjectMapper o){positionMapper=p;featureMapper=f;scoreMapper=s;objectMapper=o;}
 public HistoricalAnalysisVO analyze(Long positionId,Integer year){JobPosition p=positionMapper.selectById(positionId);if(p==null)return empty(year,"岗位不存在");JobPositionFeature f=featureMapper.selectByPositionId(positionId);JobInterviewScore score=p.getPositionCode()==null?null:scoreMapper.selectByYearAndCode(year,p.getPositionCode());return analyze(p,f,score,scoreMapper.selectAnalysisSamples(),year);}
 public HistoricalAnalysisVO analyze(JobPosition current,JobPositionFeature feature,JobInterviewScore score,List<HistoricalSampleRow> source){return analyze(current,feature,score,source,score==null?null:score.getExamYear());}
 public HistoricalAnalysisVO analyze(JobPosition current,JobPositionFeature feature,JobInterviewScore score,List<HistoricalSampleRow> source,Integer requestedYear){if(feature==null||feature.getExamSubjectGroup()==null)return empty(requestedYear,"岗位考试科目尚未可靠识别");
  LinkedHashMap<String,HistoricalSampleRow> unique=new LinkedHashMap<>();if(source!=null)for(HistoricalSampleRow r:source)if(r.getPositionCode()!=null&&r.getMinInterviewScore()!=null){String key=String.valueOf(r.getExamYear())+'|'+r.getPositionCode();if(!unique.containsKey(key))unique.put(key,r);}
  List<HistoricalSampleRow> same=new ArrayList<>();for(HistoricalSampleRow r:unique.values())if(feature.getExamSubjectGroup().equals(r.getExamSubjectGroup()))same.add(r);
  List<HistoricalSampleRow> chosen=filter(same,current,feature,1);String level="LEVEL1";if(chosen.size()<10){chosen=filter(same,current,feature,2);level="LEVEL2";}if(chosen.size()<10){chosen=filter(same,current,feature,3);level="LEVEL3";}if(chosen.size()<10){chosen=same;level="LEVEL4";}
  if(chosen.isEmpty())return empty(requestedYear,"暂无同考试口径的可比岗位样本");List<BigDecimal> values=new ArrayList<>();BigDecimal sum=BigDecimal.ZERO;for(HistoricalSampleRow r:chosen){values.add(r.getMinInterviewScore());sum=sum.add(r.getMinInterviewScore());}Collections.sort(values);BigDecimal median=values.size()%2==1?values.get(values.size()/2):values.get(values.size()/2-1).add(values.get(values.size()/2)).divide(new BigDecimal("2"),2,RoundingMode.HALF_UP);BigDecimal average=sum.divide(BigDecimal.valueOf(values.size()),2,RoundingMode.HALF_UP);String confidence=chosen.size()>=30?"HIGH":chosen.size()>=10?"MEDIUM":"LOW";
  HistoricalAnalysisVO vo=new HistoricalAnalysisVO();vo.setAvailable(true);vo.setExamYear(requestedYear);vo.setComparisonLevel(level);vo.setSampleCount(chosen.size());vo.setSampleMinScore(values.get(0));vo.setSampleMaxScore(values.get(values.size()-1));vo.setSampleMedianScore(median);vo.setSampleAverageScore(average);vo.setConfidence(confidence);vo.setReliable(!"LOW".equals(confidence));
  if(score!=null&&score.getMinInterviewScore()!=null){BigDecimal value=score.getMinInterviewScore();vo.setMinInterviewScore(value);int less=0,equal=0;for(BigDecimal sample:values){int c=sample.compareTo(value);if(c<0)less++;else if(c==0)equal++;}double percentile=(less+0.5d*equal)*100d/values.size();vo.setPercentile(Math.round(percentile*10d)/10d);vo.setRelativeLevel(relative(percentile));}
  vo.setComparisonDescription("同类岗位样本 "+chosen.size()+" 个，平均分 "+average.toPlainString()+"，中位数 "+median.toPlainString());return vo;
 }
 private List<HistoricalSampleRow> filter(List<HistoricalSampleRow> rows,JobPosition p,JobPositionFeature f,int level){List<HistoricalSampleRow> out=new ArrayList<>();for(HistoricalSampleRow r:rows){boolean domain=domainSimilar(f.getMajorRestrictionType(),f.getMajorDomains(),r.getMajorRestrictionType(),r.getMajorDomains());if(level<=3&&!domain)continue;if(level==1&&(!Objects.equals(f.getMajorRestrictionType(),r.getMajorRestrictionType())||bucket(p.getRecruitCount())!=bucket(r.getRecruitCount())))continue;if(level==2&&!family(f.getMajorRestrictionType()).equals(family(r.getMajorRestrictionType())))continue;out.add(r);}return out;}
 private boolean domainSimilar(String ta,String ja,String tb,String jb){if("UNRESTRICTED".equals(ta)&&"UNRESTRICTED".equals(tb))return true;Set<String>a=new HashSet<>(list(ja)),b=new HashSet<>(list(jb));a.retainAll(b);return !a.isEmpty();}
 private List<String> list(String j){try{return objectMapper.readValue(j,new TypeReference<List<String>>(){});}catch(Exception e){return Collections.emptyList();}}
 private String family(String t){if(t==null)return "UNCERTAIN";if(t.contains("EXACT"))return "EXACT";if(t.contains("CATEGORY"))return "CATEGORY";return t;}
 private int bucket(Integer n){if(n==null||n<=1)return 1;if(n==2)return 2;if(n<=4)return 3;return 4;}
 private String relative(double p){if(p<25)return "LOWER";if(p<50)return "LOWER_MIDDLE";if(p<75)return "UPPER_MIDDLE";return "HIGHER";}
 private HistoricalAnalysisVO empty(Integer year,String desc){HistoricalAnalysisVO v=new HistoricalAnalysisVO();v.setExamYear(year);v.setAvailable(false);v.setConfidence("LOW");v.setReliable(false);v.setComparisonDescription(desc);return v;}
}
