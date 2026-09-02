package com.gk.jobhelper.service;

import com.gk.jobhelper.common.BusinessException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DocxResumeTextExtractor implements ResumeTextExtractor {
    private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public boolean supports(String fileName, String contentType) {
        return fileName != null && fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".docx")
                && DOCX_CONTENT_TYPE.equalsIgnoreCase(contentType);
    }

    @Override
    public String extract(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream); XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (IOException e) {
            throw new BusinessException("DOCX 简历无法读取，请确认文件未损坏或重新另存为 DOCX 后再上传。");
        }
    }
}
