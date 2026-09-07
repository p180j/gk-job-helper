package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.RecruitmentPosition;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class HtmlRecruitmentPositionExtractor {
 private static final Pattern FIELD=Pattern.compile("(?m)^\\s*(岗位名称|职位名称|招聘岗位|招聘单位|单位|部门|招聘人数|计划人数|人数|工作地点|地点|学历要求|学历|学位要求|学位|专业要求|专业|年龄要求|年龄|工作经验|工作年限|岗位职责|职位描述|任职要求|资格条件|招聘条件|其他要求)\\s*[：:]\\s*(.+?)\\s*$");
 private final RecruitmentExcelHeaderNormalizer normalizer;
 public HtmlRecruitmentPositionExtractor(RecruitmentExcelHeaderNormalizer normalizer){this.normalizer=normalizer;}

 public List<RecruitmentPosition> extract(String html,String text,Long noticeId){
  List<RecruitmentPosition> result=fromTables(html,noticeId);
  if(!result.isEmpty())return result;
  return fromText(text,noticeId);
 }

 private List<RecruitmentPosition> fromTables(String html,Long noticeId){
  List<RecruitmentPosition> out=new ArrayList<>(); if(html==null||html.trim().isEmpty())return out;
  Elements tables=Jsoup.parse(html).select("table");
  for(int tableIndex=0;tableIndex<tables.size();tableIndex++){
   List<List<String>> grid=grid(tables.get(tableIndex)); int header=findHeader(grid); if(header<0)continue;
   Map<Integer,String> columns=columns(grid.get(header));
   for(int row=header+1;row<grid.size();row++){
    Map<String,String> data=new LinkedHashMap<>(); List<String> cells=grid.get(row);
    for(Map.Entry<Integer,String> column:columns.entrySet())data.put(column.getValue(),column.getKey()<cells.size()?trim(cells.get(column.getKey())):"");
    if(skip(data.get("POSITION_NAME"),data))continue;
    out.add(position(data,noticeId,"BODY_HTML","table["+(tableIndex+1)+"]",row+1,raw(data)));
   }
  }
  return out;
 }

 private List<List<String>> grid(Element table){
  List<List<String>> rows=new ArrayList<>(); Map<Integer,Span> spans=new HashMap<>();
  for(Element tr:table.select("tr")){
   List<String> row=new ArrayList<>(); int col=0;
   for(Element cell:tr.select("> th, > td")){
    while(spans.containsKey(col)){Span span=spans.get(col);put(row,col,span.text);if(--span.left==0)spans.remove(col);col++;}
    String value=cell.text().trim();int colspan=positive(cell.attr("colspan")),rowspan=positive(cell.attr("rowspan"));
    for(int i=0;i<colspan;i++){put(row,col,value);if(rowspan>1)spans.put(col,new Span(value,rowspan-1));col++;}
   }
   while(spans.containsKey(col)){Span span=spans.get(col);put(row,col,span.text);if(--span.left==0)spans.remove(col);col++;}
   if(!row.isEmpty())rows.add(row);
  }
  return rows;
 }
 private int findHeader(List<List<String>> rows){for(int i=0;i<Math.min(rows.size(),60);i++){Set<String> found=new HashSet<>();for(String cell:rows.get(i)){String field=normalizer.normalize(cell);if(field!=null)found.add(field);}if(found.contains("POSITION_NAME")&&found.size()>=3)return i;}return -1;}
 private Map<Integer,String> columns(List<String> row){Map<Integer,String> result=new LinkedHashMap<>();for(int i=0;i<row.size();i++){String field=normalizer.normalize(row.get(i));if(field!=null&&!result.containsValue(field))result.put(i,field);}return result;}

 private List<RecruitmentPosition> fromText(String text,Long noticeId){
  List<RecruitmentPosition> out=new ArrayList<>();if(text==null)return out;String normalized=text.replace('\r','\n').replaceAll("\\n{2,}","\n");
  Matcher matcher=FIELD.matcher(normalized);Map<String,String> data=new LinkedHashMap<>();int block=1,start=0;
  while(matcher.find()){
   String field=normalizer.normalize(matcher.group(1));if(field==null)continue;
   if("POSITION_NAME".equals(field)&&data.containsKey("POSITION_NAME")){addText(out,data,noticeId,block++,start,normalized.substring(start,matcher.start()));data=new LinkedHashMap<>();start=matcher.start();}
   data.put(field,trim(matcher.group(2)));
  }
  addText(out,data,noticeId,block,start,normalized.substring(Math.min(start,normalized.length())));return out;
 }
 private void addText(List<RecruitmentPosition> out,Map<String,String> data,Long noticeId,int block,int start,String raw){if(data.size()<3||skip(data.get("POSITION_NAME"),data))return;out.add(position(data,noticeId,"BODY_TEXT","section["+block+"]",lineAt(raw,start),raw.trim()));}
 private RecruitmentPosition position(Map<String,String> d,Long noticeId,String sourceType,String source,int row,String raw){RecruitmentPosition p=new RecruitmentPosition();p.setNoticeId(noticeId);p.setSourceType(sourceType);p.setPositionName(d.get("POSITION_NAME"));p.setPositionCode(d.get("POSITION_CODE"));p.setOrganizationName(d.get("ORGANIZATION"));p.setDepartmentName(d.get("DEPARTMENT"));p.setRecruitCount(number(d.get("RECRUIT_COUNT")));p.setWorkLocation(d.get("WORK_LOCATION"));p.setEducationRequirement(d.get("EDUCATION"));p.setDegreeRequirement(d.get("DEGREE"));p.setMajorRequirement(d.get("MAJOR"));p.setAgeRequirement(d.get("AGE"));p.setWorkYearsRequirement(d.get("WORK_EXPERIENCE"));p.setResponsibility(d.get("RESPONSIBILITY"));p.setOtherRequirement(join(d.get("REQUIREMENT"),d.get("OTHER")));p.setPreferredRequirement(d.get("PREFERRED"));p.setRawRequirement(raw);p.setSourceSheet(source);p.setSourceRow(row);p.setCreatedAt(LocalDateTime.now());p.setUpdatedAt(LocalDateTime.now());return p;}
 private boolean skip(String name,Map<String,String> data){String n=trim(name);if(n.isEmpty()||n.matches("^(合计|总计|备注|说明|注|注释|附件).*$"))return true;return raw(data).matches("^(合计|总计|备注|说明|注|注释).*");}
 private String raw(Map<String,String> data){StringBuilder value=new StringBuilder();for(Map.Entry<String,String> e:data.entrySet())if(!trim(e.getValue()).isEmpty()){if(value.length()>0)value.append("；");value.append(e.getKey()).append("：").append(e.getValue());}return value.toString();}
 private int positive(String value){try{return Math.max(1,Integer.parseInt(value));}catch(Exception e){return 1;}}
 private int lineAt(String value,int index){int line=1;for(int i=0;i<Math.min(index,value.length());i++)if(value.charAt(i)=='\n')line++;return line;}
 private Integer number(String value){try{String n=trim(value).replaceAll("[^0-9]","");return n.isEmpty()?null:Integer.valueOf(n);}catch(Exception e){return null;}}
 private String trim(String value){return value==null?"":value.trim();}
 private String join(String first,String second){if(trim(first).isEmpty())return trim(second).isEmpty()?null:trim(second);if(trim(second).isEmpty())return trim(first);return trim(first)+"；"+trim(second);}
 private void put(List<String> row,int index,String value){while(row.size()<=index)row.add("");row.set(index,value);}
 private static class Span {private final String text;private int left;private Span(String text,int left){this.text=text;this.left=left;}}
}
