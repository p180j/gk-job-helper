package com.gk.jobhelper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.entity.JobPosition;
import org.springframework.stereotype.Component;
import java.util.*; import java.util.regex.*;

@Component
public class ExamSubjectResolver {
 private final ObjectMapper objectMapper;
 private static final Pattern NUMBER=Pattern.compile("(\\d+|[一二两三四五六])");
 public ExamSubjectResolver(ObjectMapper objectMapper){this.objectMapper=objectMapper;}
 public Result resolve(JobPosition p){
  Map<String,String> raw=read(p.getRawData()); String text=value(raw,"考试科目","笔试科目","公共科目");
  Integer declared=parseCount(value(raw,"科目数量","考试科目数量")); List<String> subjects=parseSubjects(text);
  Result r=new Result(); r.rawText=text; r.subjects=subjects; r.count=subjects.isEmpty()?declared:Integer.valueOf(subjects.size());
  if(subjects.isEmpty()){r.status="UNKNOWN";return r;}
  if(declared!=null&&declared.intValue()!=subjects.size()){r.status="UNCERTAIN";return r;}
  r.status="RECOGNIZED"; List<String> signature=new ArrayList<>(subjects); Collections.sort(signature);
  r.group=String.join("|",signature); return r;
 }
 private Map<String,String> read(String json){try{return objectMapper.readValue(json,new TypeReference<Map<String,String>>(){});}catch(Exception e){return Collections.emptyMap();}}
 private String value(Map<String,String> m,String... keys){for(String k:keys)for(Map.Entry<String,String> e:m.entrySet())if(e.getKey()!=null&&e.getKey().replaceAll("\\s","").equals(k)&&e.getValue()!=null&&!e.getValue().trim().isEmpty())return e.getValue().trim();return null;}
 private Integer parseCount(String s){if(s==null)return null;Matcher m=NUMBER.matcher(s);if(!m.find())return null;String v=m.group(1);if(v.matches("\\d+"))return Integer.valueOf(v);if("一".equals(v))return 1;if("二".equals(v)||"两".equals(v))return 2;if("三".equals(v))return 3;if("四".equals(v))return 4;if("五".equals(v))return 5;return 6;}
 private List<String> parseSubjects(String s){if(s==null)return Collections.emptyList();String normalized=s.replace('，',',').replace('、',',').replace('；',',').replace(';',',');LinkedHashSet<String> out=new LinkedHashSet<>();
  for(String item:normalized.split(",")){String v=item.trim().replace("《","").replace("》","");if(v.isEmpty())continue;
   if(v.equals("行测")||v.contains("行政职业能力测验"))v="行政职业能力测验";
   else if(v.startsWith("申论")){if(v.contains("行政执法"))v="申论（行政执法类）";else if(v.contains("省市")||v.contains("综合管理"))v="申论（省市综合管理类）";else if(v.contains("县乡"))v="申论（县乡综合管理类）";else v="申论";}
   out.add(v);
  }return new ArrayList<>(out);
 }
 public static class Result {private Integer count;private List<String> subjects=Collections.emptyList();private String group;private String status;private String rawText;
  public Integer getCount(){return count;}public List<String> getSubjects(){return subjects;}public String getGroup(){return group;}public String getStatus(){return status;}public String getRawText(){return rawText;}}
}
