package com.gk.jobhelper.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;import org.apache.poi.xwpf.usermodel.*;import org.junit.jupiter.api.Test;

class DocxRecruitmentPositionExtractorTest {
 private final RecruitmentPositionFieldMapper mapper=new RecruitmentPositionFieldMapper();private final RecruitmentPositionTextExtractor text=new RecruitmentPositionTextExtractor(new RecruitmentExcelHeaderNormalizer(),mapper);private final DocxRecruitmentPositionExtractor extractor=new DocxRecruitmentPositionExtractor(new RecruitmentExcelHeaderNormalizer(),mapper,text);
 @Test void readsMultipleTablesAndKeepsMissingFieldsEmpty()throws Exception{RecruitmentPositionExtractionResult result=extractor.extract(new DownloadedAttachment(8L,"jobs.docx","","DOCX",tableDocx()),6L);assertEquals(2,result.positions.size());assertEquals("研发工程师",result.positions.get(0).getPositionName());assertEquals("产品专员",result.positions.get(1).getPositionName());assertNull(result.positions.get(1).getEducationRequirement());}
 @Test void readsReliableParagraphBlocksAndLevelledMajor()throws Exception{RecruitmentPositionExtractionResult result=extractor.extract(new DownloadedAttachment(8L,"jobs.docx","","DOCX",paragraphDocx()),6L);assertEquals(1,result.positions.size());assertTrue(result.positions.get(0).getMajorRequirement().contains("本科专业：计算机类"));assertTrue(result.positions.get(0).getMajorRequirement().contains("研究生专业：电子信息"));}
 @Test void doesNotInventPositionFromNotes()throws Exception{assertTrue(extractor.extract(new DownloadedAttachment(8L,"jobs.docx","","DOCX",notesDocx()),6L).positions.isEmpty());}
 private byte[] tableDocx()throws Exception{try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){table(doc,"岗位名称","招聘人数","学历","研发工程师","2","本科");table(doc,"岗位名称","招聘人数","专业","产品专员","1","管理类");doc.write(out);return out.toByteArray();}}
 private byte[] paragraphDocx()throws Exception{try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){for(String line:"岗位名称：算法工程师\n招聘人数：1\n本科专业：计算机类\n研究生专业：电子信息\n任职要求：本科及以上学历；3年以上工作经验；年龄35周岁以下".split("\\n"))doc.createParagraph().createRun().setText(line);doc.write(out);return out.toByteArray();}}
 private byte[] notesDocx()throws Exception{try(XWPFDocument doc=new XWPFDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){doc.createParagraph().createRun().setText("说明：请以公告为准");doc.write(out);return out.toByteArray();}}
 private void table(XWPFDocument doc,String... values){XWPFTable table=doc.createTable(2,3);for(int i=0;i<3;i++){table.getRow(0).getCell(i).setText(values[i]);table.getRow(1).getCell(i).setText(values[i+3]);}}
}
