package com.gk.jobhelper.service;

import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.entity.RecruitmentPosition;
import java.io.ByteArrayInputStream;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfRecruitmentPositionExtractor implements RecruitmentPositionExtractor {
    private final RecruitmentPositionTextExtractor textExtractor;
    public PdfRecruitmentPositionExtractor(RecruitmentPositionTextExtractor textExtractor) { this.textExtractor = textExtractor; }
    @Override public boolean supports(String fileType) { return "PDF".equals(fileType); }
    @Override public RecruitmentPositionExtractionResult extract(DownloadedAttachment attachment, Long noticeId) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(attachment.getBytes()))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<RecruitmentPosition> positions = new ArrayList<>(); boolean hasText = false;
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page); stripper.setEndPage(page);
                String text = stripper.getText(document).trim();
                if (text.isEmpty()) continue;
                hasText = true;
                positions.addAll(textExtractor.extract(text, noticeId, attachment.getAttachmentId(), "ATTACHMENT_PDF", "page[" + page + "]"));
            }
            if (!hasText) throw new OcrRequiredException();
            return new RecruitmentPositionExtractionResult(positions, document.getNumberOfPages(), Collections.emptyMap());
        } catch (OcrRequiredException e) { throw e;
        } catch (Exception e) { throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "PDF岗位附件读取失败：" + e.getMessage()); }
    }
}
