package com.gk.jobhelper.service;

import com.gk.jobhelper.common.MajorNameNormalizer; import com.gk.jobhelper.dto.MajorSearchItemVO;
import com.gk.jobhelper.entity.JobPosition; import com.gk.jobhelper.matcher.MajorRequirementParser; import com.gk.jobhelper.matcher.RequirementToken;
import org.springframework.stereotype.Component; import java.util.*;

@Component
public class MajorRestrictionAnalyzer {
 private final MajorCatalogService catalogService;
 public MajorRestrictionAnalyzer(MajorCatalogService catalogService){this.catalogService=catalogService;}
 public Result analyze(JobPosition p){
  MajorRequirementParser.ParsedRequirement parsed=MajorRequirementParser.parse(p.getMajorRequirement(),p.getEducationRequirement());
  if(parsed.isUnlimited())return new Result("UNRESTRICTED",Collections.<String>emptyList(),null,"RECOGNIZED");
  if(parsed.isParseFailure())return uncertain();
  int exact=0,categories=0,scope=0; LinkedHashSet<String> domains=new LinkedHashSet<>();
  for(RequirementToken token:parsed.getTokens()){
   if(token.isRelatedOnly()||token.isRelatedSuffix()||token.isOpaque())return uncertain();
   String keyword=token.getCode()!=null?token.getCode():token.getName(); if(keyword==null)return uncertain();
   List<MajorSearchItemVO> found=catalogService.search(keyword,20); MajorSearchItemVO hit=exactHit(found,token); if(hit==null)return uncertain();
   String level=hit.getItemLevel(); boolean category="CATEGORY".equals(level)||"CLASS".equals(level);
   if(category)categories++;else exact++; scope++;
   String domain=hit.getParentName()!=null?hit.getParentName():hit.getMajorName(); if(domain!=null)domains.add(domain);
  }
  String type; int total=exact+categories;
  if(exact>0&&categories>0)return uncertain();
  if(exact>0)type=total==1?"EXACT_MAJOR":"MULTI_EXACT_MAJOR";else type=total==1?"MAJOR_CATEGORY":"MULTI_CATEGORY";
  return new Result(type,new ArrayList<>(domains),scope,"RECOGNIZED");
 }
 private MajorSearchItemVO exactHit(List<MajorSearchItemVO> rows,RequirementToken token){for(MajorSearchItemVO r:rows){if(token.getCode()!=null&&token.getCode().replaceAll("[^0-9A-Za-z]","").equalsIgnoreCase(r.getMajorCode()))return r;if(token.getName()!=null&&MajorNameNormalizer.comparisonName(token.getName()).equals(MajorNameNormalizer.comparisonName(r.getMajorName())))return r;}return null;}
 private Result uncertain(){return new Result("UNCERTAIN",Collections.<String>emptyList(),null,"UNKNOWN");}
 public static class Result {private final String type;private final List<String> domains;private final Integer scopeCount;private final String status;
  Result(String t,List<String>d,Integer c,String s){type=t;domains=d;scopeCount=c;status=s;} public String getType(){return type;}public List<String> getDomains(){return domains;}public Integer getScopeCount(){return scopeCount;}public String getStatus(){return status;}}
}
