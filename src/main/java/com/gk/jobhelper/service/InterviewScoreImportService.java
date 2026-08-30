package com.gk.jobhelper.service;

import com.gk.jobhelper.common.BusinessException; import com.gk.jobhelper.dto.*; import com.gk.jobhelper.entity.*; import com.gk.jobhelper.mapper.*;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.multipart.MultipartFile;
import java.io.*; import java.math.BigDecimal; import java.nio.file.*; import java.time.LocalDateTime; import java.util.*;

@Service
public class InterviewScoreImportService {
 private final ExcelRowReader reader; private final JobInterviewScoreMapper scoreMapper; private final JobPositionMapper positionMapper;
 public InterviewScoreImportService(ExcelRowReader r,JobInterviewScoreMapper s,JobPositionMapper p){reader=r;scoreMapper=s;positionMapper=p;}
 @Transactional public InterviewScoreImportResult importFile(Integer year,Long importId,MultipartFile upload){if(year==null||year<2000||year>2100)throw new BusinessException("examYear 不合法");if(upload==null||upload.isEmpty())throw new BusinessException("请选择进面名单 Excel");Path temp=null;
  try{String suffix=upload.getOriginalFilename()!=null&&upload.getOriginalFilename().toLowerCase().endsWith(".xlsx")?".xlsx":".xls";temp=Files.createTempFile("gk-interview-",suffix);upload.transferTo(temp.toFile());return importFile(year,importId,temp.toFile(),upload.getOriginalFilename());}
  catch(IOException e){throw new BusinessException("进面名单读取失败: "+e.getMessage());}finally{if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
 }
 @Transactional public InterviewScoreImportResult importFile(Integer year,File file,String sourceName){return importFile(year,null,file,sourceName);}
 @Transactional public InterviewScoreImportResult importFile(Integer year,Long importId,File file,String sourceName){ExcelRawSheet sheet=reader.read(file,null);Map<String,Integer> columns=columns(sheet.getHeaders());int codeCol=require(columns,"positioncode","职位代码");int scoreCol=require(columns,"score","笔试成绩/进面分");Integer deptCol=find(columns,"department");Integer nameCol=find(columns,"positionname");
  InterviewScoreImportResult result=new InterviewScoreImportResult();result.setImportId(importId);result.setExamYear(year);result.setRawRowCount(sheet.getRows().size());Map<String,JobInterviewScore> grouped=new LinkedHashMap<>();
  for(ExcelRawRow row:sheet.getRows()){String code=code(row.getCells().get(codeCol));BigDecimal score=decimal(row.getCells().get(scoreCol));if(code==null||score==null){result.setInvalidRowCount(result.getInvalidRowCount()+1);if(result.getInvalidRows().size()<50)result.getInvalidRows().add("第"+row.getRowNumber()+"行：职位代码或分数无效");continue;}
   JobInterviewScore item=grouped.get(code);if(item==null){item=new JobInterviewScore();item.setExamYear(year);item.setPositionCode(code);item.setDepartmentName(cell(row,deptCol));item.setPositionName(cell(row,nameCol));item.setMinInterviewScore(score);item.setInterviewCandidateCount(1);item.setSourceFileName(sourceName==null?file.getName():sourceName);item.setCreatedAt(LocalDateTime.now());grouped.put(code,item);}else{if(score.compareTo(item.getMinInterviewScore())<0)item.setMinInterviewScore(score);item.setInterviewCandidateCount(item.getInterviewCandidateCount()+1);}item.setUpdatedAt(LocalDateTime.now());
  }
  List<String> codes=new ArrayList<>(grouped.keySet());Set<String> existingCodes=new HashSet<>();if(!codes.isEmpty()){List<JobInterviewScore> existing=scoreMapper.selectByYearAndCodes(year,codes);if(existing!=null)for(JobInterviewScore score:existing)if(score.getPositionCode()!=null)existingCodes.add(code(score.getPositionCode()));}
  result.setInsertedCount(grouped.size()-existingCodes.size());result.setUpdatedCount(existingCodes.size());for(JobInterviewScore item:grouped.values())scoreMapper.upsert(item);result.setAggregatedPositionCount(grouped.size());Set<String> linked=new HashSet<>();if(!grouped.isEmpty()){List<JobPosition> positions=importId==null?positionMapper.selectByPositionCodes(codes):positionMapper.selectByImportFileIdAndPositionCodes(importId,codes);if(positions!=null)for(JobPosition p:positions)if(p.getPositionCode()!=null)linked.add(code(p.getPositionCode()));}result.setLinkedPositionCount(linked.size());result.setUnlinkedPositionCount(grouped.size()-linked.size());return result;
 }
 private Map<String,Integer> columns(List<String> headers){Map<String,Integer> m=new HashMap<>();for(int i=0;i<headers.size();i++){String h=norm(headers.get(i));m.put(h,i);if(InterviewScoreHeaderPolicy.isPositionCode(h))m.put("positioncode",i);if(InterviewScoreHeaderPolicy.isScore(h))m.put("score",i);if(InterviewScoreHeaderPolicy.isDepartment(h))m.put("department",i);if(InterviewScoreHeaderPolicy.isPositionName(h))m.put("positionname",i);}return m;}
 private int require(Map<String,Integer> m,String key,String label){Integer v=m.get(key);if(v==null)throw new BusinessException("Excel 缺少"+label+"列");return v;}private Integer find(Map<String,Integer>m,String k){return m.get(k);}
 private String norm(String s){return s==null?"":s.replaceAll("[\\s　]","").replace("（","").replace("）","").replace("(","").replace(")","");}
 private String cell(ExcelRawRow r,Integer i){if(i==null)return null;String v=r.getCells().get(i);return v==null||v.trim().isEmpty()?null:v.trim();}
 private String code(String s){if(s==null)return null;String v=s.trim().replaceAll("\\s","");if(v.endsWith(".0"))v=v.substring(0,v.length()-2);return v.isEmpty()?null:v;}
 private BigDecimal decimal(String s){if(s==null)return null;String v=s.replace(",","").replaceAll("[^0-9.\\-]","");try{return v.isEmpty()?null:new BigDecimal(v);}catch(Exception e){return null;}}
}
