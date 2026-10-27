package com.gk.jobhelper.service;

import com.fasterxml.jackson.core.type.TypeReference; import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.common.*; import com.gk.jobhelper.dto.*; import com.gk.jobhelper.entity.*; import com.gk.jobhelper.mapper.*;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.util.*; import java.util.function.Consumer;

@Service
public class JobFeatureService {
 private final JobPositionMapper positionMapper; private final JobPositionFeatureMapper featureMapper;
 private final ExamSubjectResolver subjectResolver; private final MajorRestrictionAnalyzer majorAnalyzer; private final ObjectMapper objectMapper;
 private final JobMajorRequirementResolver majorRequirementResolver; private final JobRemarkParser remarkParser;
 private final JobWorkLocationResolver workLocationResolver;
 private final JobFeatureSnapshotService snapshots;
 public JobFeatureService(JobPositionMapper p,JobPositionFeatureMapper f,ExamSubjectResolver s,MajorRestrictionAnalyzer m,ObjectMapper o,
                          JobMajorRequirementResolver mr,JobRemarkParser rp,JobWorkLocationResolver lr,JobFeatureSnapshotService snapshots){positionMapper=p;featureMapper=f;subjectResolver=s;majorAnalyzer=m;objectMapper=o;majorRequirementResolver=mr;remarkParser=rp;workLocationResolver=lr;this.snapshots=snapshots;}
 @Transactional public FeatureRebuildResult rebuild(Long importId){return rebuild(importId,result -> { });}
 public FeatureRebuildResult previewParsed(Long importId){return rebuildParsed(importId,true);}
 @Transactional public FeatureRebuildResult applyParsed(Long importId){return rebuildParsed(importId,false);}
 private FeatureRebuildResult rebuildParsed(Long importId,boolean preview){
  if(importId==null)throw new BusinessException("importId 不能为空");
  List<JobPosition> positions=positionMapper.selectByImportFileId(importId);
  FeatureRebuildResult result=new FeatureRebuildResult();result.setImportId(importId);result.setTotal(positions.size());result.setPreview(preview);
  for(JobPosition position:positions){JobPositionFeature current=featureMapper.selectByPositionId(position.getId());
   JobPositionFeature next=build(position);countLocation(result,next.getWorkLocationStatus());
   boolean changed=!JobFeatureSnapshotService.sameParsedFields(current,next);
   if(changed){result.setChanged(result.getChanged()+1);if(!preview){
    if(result.getBackupId()==null)result.setBackupId(snapshots.create(importId,positions));
    if(current==null)featureMapper.upsert(next);else featureMapper.updateParsedFields(next);}}
   result.setSuccess(result.getSuccess()+1);
  }return result;
 }
 private void countLocation(FeatureRebuildResult result,String status){
  if("CONFIRMED".equals(status))result.setConfirmedLocations(result.getConfirmedLocations()+1);
  else if("INFERRED".equals(status))result.setInferredLocations(result.getInferredLocations()+1);
  else if("MULTI_SITE".equals(status))result.setMultiSiteLocations(result.getMultiSiteLocations()+1);
  else result.setUnknownLocations(result.getUnknownLocations()+1);
 }
 @Transactional public FeatureRebuildResult rebuild(Long importId,Consumer<FeatureRebuildResult> progressReporter){if(importId==null)throw new BusinessException("importId 不能为空");List<JobPosition> positions=positionMapper.selectByImportFileId(importId);FeatureRebuildResult result=new FeatureRebuildResult();result.setImportId(importId);result.setTotal(positions.size());progressReporter.accept(result);
  for(JobPosition p:positions){JobPositionFeature f=build(p);featureMapper.upsert(f);result.setSuccess(result.getSuccess()+1);if("UNKNOWN".equals(f.getExamSubjectStatus()))result.setUnknownSubjects(result.getUnknownSubjects()+1);if("UNCERTAIN".equals(f.getMajorRestrictionType()))result.setUncertainMajors(result.getUncertainMajors()+1);if(result.getSuccess()%100==0||result.getSuccess()==result.getTotal())progressReporter.accept(result);}return result;}
 public JobFeatureVO get(Long positionId){if(positionMapper.selectById(positionId)==null)throw new BusinessException(ApiResponse.CODE_JOB_NOT_FOUND,"岗位不存在: "+positionId);JobPositionFeature f=featureMapper.selectByPositionId(positionId);return f==null?null:toVO(f);}
 public JobPositionFeature build(JobPosition p){ExamSubjectResolver.Result s=subjectResolver.resolve(p);MajorRestrictionAnalyzer.Result m=majorAnalyzer.analyze(p);LocalDateTime now=LocalDateTime.now();JobPositionFeature f=new JobPositionFeature();f.setPositionId(p.getId());f.setExamSubjectCount(s.getCount());f.setExamSubjects(json(s.getSubjects()));f.setExamSubjectGroup(s.getGroup());f.setExamSubjectStatus(s.getStatus());f.setRawExamSubjectText(s.getRawText());f.setMajorRestrictionType(m.getType());f.setMajorDomains(json(m.getDomains()));f.setMajorSimilarityKeys(json(m.getSimilarityKeys()));f.setMajorScopeCount(m.getScopeCount());f.setMajorAnalysisStatus(m.getStatus());String level=organizationLevel(p);f.setOrganizationLevel(level);f.setOrganizationLevelStatus(level==null?"UNKNOWN":"RECOGNIZED");f.setEffectiveMajorRequirement(majorRequirementResolver.resolve(p));f.setRemarkConditions(json(remarkParser.parse(p)));JobWorkLocationResolver.Result location=workLocationResolver.resolve(p);f.setWorkLocationName(location.getName());f.setWorkLocationCode(location.getCode());f.setWorkLocationStatus(location.getStatus());f.setWorkLocationSource(location.getSource());f.setWorkLocationEvidence(location.getEvidence());f.setWorkLocationType(location.getType());f.setCreatedAt(now);f.setUpdatedAt(now);return f;}
 public JobFeatureVO toVO(JobPositionFeature f){JobFeatureVO v=new JobFeatureVO();v.setPositionId(f.getPositionId());v.setExamSubjectCount(f.getExamSubjectCount());v.setExamSubjects(list(f.getExamSubjects()));v.setExamSubjectGroup(f.getExamSubjectGroup());v.setExamSubjectStatus(f.getExamSubjectStatus());v.setRawExamSubjectText(f.getRawExamSubjectText());v.setMajorRestrictionType(f.getMajorRestrictionType());v.setMajorDomains(list(f.getMajorDomains()));v.setMajorSimilarityKeys(list(f.getMajorSimilarityKeys()));v.setMajorScopeCount(f.getMajorScopeCount());v.setMajorAnalysisStatus(f.getMajorAnalysisStatus());v.setOrganizationLevel(f.getOrganizationLevel());v.setEffectiveMajorRequirement(f.getEffectiveMajorRequirement());v.setRemarkConditions(f.getRemarkConditions());v.setWorkLocationName(f.getWorkLocationName());v.setWorkLocationCode(f.getWorkLocationCode());v.setWorkLocationStatus(f.getWorkLocationStatus());v.setWorkLocationSource(f.getWorkLocationSource());v.setWorkLocationEvidence(f.getWorkLocationEvidence());v.setWorkLocationType(f.getWorkLocationType());return v;}
 private String organizationLevel(JobPosition p){String t=((p.getDepartmentName()==null?"":p.getDepartmentName())+(p.getOrganizationName()==null?"":p.getOrganizationName()));if(t.contains("省"))return "PROVINCE";if(t.contains("市"))return "CITY";if(t.contains("县")||t.contains("区"))return "COUNTY";if(t.contains("乡")||t.contains("镇")||t.contains("街道"))return "TOWNSHIP";return null;}
 private String json(Object v){try{return objectMapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException("岗位派生特征序列化失败",e);}}
 private List<String> list(String json){try{return objectMapper.readValue(json,new TypeReference<List<String>>(){});}catch(Exception e){return new ArrayList<>();}}
}
