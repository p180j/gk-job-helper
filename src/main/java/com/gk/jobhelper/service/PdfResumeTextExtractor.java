package com.gk.jobhelper.service;

import com.gk.jobhelper.common.BusinessException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfResumeTextExtractor implements ResumeTextExtractor {
    @Override
    public boolean supports(String fileName, String contentType) {
        return fileName != null && fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
                && "application/pdf".equalsIgnoreCase(contentType);
    }

    @Override
    public String extract(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new BusinessException("PDF 简历无法读取，请确认文件未损坏且不是扫描版图片。");
        }
    }
}
