package com.gk.jobhelper.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import org.apache.pdfbox.pdmodel.*;import org.apache.pdfbox.pdmodel.font.PDType1Font;import org.apache.pdfbox.pdmodel.PDPageContentStream;import org.junit.jupiter.api.Test;

class PdfRecruitmentPositionExtractorTest {
 private final RecruitmentPositionFieldMapper mapper=new RecruitmentPositionFieldMapper();private final RecruitmentPositionTextExtractor text=new RecruitmentPositionTextExtractor(new RecruitmentExcelHeaderNormalizer(),mapper);private final PdfRecruitmentPositionExtractor extractor=new PdfRecruitmentPositionExtractor(text);
 @Test void readsTextPdfWithoutTreatingItAsScan()throws Exception{RecruitmentPositionExtractionResult result=extractor.extract(new DownloadedAttachment(5L,"jobs.pdf","application/pdf","PDF",pdf("Text PDF attachment")),7L);assertTrue(result.positions.isEmpty());}
 @Test void flagsImageOnlyPdf(){assertThrows(OcrRequiredException.class,()->extractor.extract(new DownloadedAttachment(5L,"scan.pdf","application/pdf","PDF",emptyPdf()),7L));}
 private byte[] pdf(String value)throws Exception{try(PDDocument document=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){document.addPage(new PDPage());try(PDPageContentStream content=new PDPageContentStream(document,document.getPage(0))){content.beginText();content.setFont(PDType1Font.HELVETICA,12);content.newLineAtOffset(30,700);content.showText(value);content.endText();}document.save(out);return out.toByteArray();}}
 private byte[] emptyPdf()throws Exception{try(PDDocument document=new PDDocument();ByteArrayOutputStream out=new ByteArrayOutputStream()){document.addPage(new PDPage());document.save(out);return out.toByteArray();}}
}
