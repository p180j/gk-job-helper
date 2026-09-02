package com.gk.jobhelper.service;
import java.util.Locale;
import org.springframework.stereotype.Component;
@Component public class RecruitmentAttachmentClassifier {
 public String fileType(String fileName){String n=fileName==null?"":fileName.toLowerCase(Locale.ROOT);if(n.endsWith(".xlsx"))return "XLSX";if(n.endsWith(".xls"))return "XLS";if(n.endsWith(".docx"))return "DOCX";if(n.endsWith(".doc"))return "DOC";if(n.endsWith(".pdf"))return "PDF";if(n.endsWith(".zip"))return "ZIP";return "OTHER";}
 public String attachmentType(String fileName){String n=fileName==null?"":fileName.replaceAll("\\s","");if(contains(n,"岗位表","职位表","招聘计划","岗位计划","岗位信息","招聘岗位","资格条件"))return "POSITION_DATA";if(contains(n,"报名表","应聘登记表","应聘人员信息表","报名登记表"))return "APPLICATION_FORM";if(contains(n,"承诺书","诚信承诺"))return "COMMITMENT";if(contains(n,"操作指南","报名指南","招聘说明","使用说明"))return "GUIDE";return "OTHER";}
 private boolean contains(String text,String... words){for(String word:words)if(text.contains(word))return true;return false;}
}
