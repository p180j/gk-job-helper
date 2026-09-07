package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.entity.RecruitmentPosition;
import java.io.*;
import java.util.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

@Component
public class DocxRecruitmentPositionExtractor implements RecruitmentPositionExtractor {
    private final RecruitmentExcelHeaderNormalizer normalizer; private final RecruitmentPositionFieldMapper mapper; private final RecruitmentPositionTextExtractor textExtractor;
    public DocxRecruitmentPositionExtractor(RecruitmentExcelHeaderNormalizer normalizer, RecruitmentPositionFieldMapper mapper, RecruitmentPositionTextExtractor textExtractor) { this.normalizer = normalizer; this.mapper = mapper; this.textExtractor = textExtractor; }
    @Override public boolean supports(String fileType) { return "DOCX".equals(fileType); }
    @Override public RecruitmentPositionExtractionResult extract(DownloadedAttachment attachment, Long noticeId) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(attachment.getBytes()))) {
            List<RecruitmentPosition> positions = new ArrayList<>(); Map<String, Integer> headers = new LinkedHashMap<>();
            List<XWPFTable> tables = document.getTables();
            for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                List<List<String>> rows = rows(tables.get(tableIndex)); int header = findHeader(rows);
                if (header < 0) continue; headers.put("table[" + (tableIndex + 1) + "]", header + 1);
                Map<Integer, String> columns = columns(rows.get(header));
                for (int row = header + 1; row < rows.size(); row++) {
                    Map<String, String> data = new LinkedHashMap<>(); List<String> cells = rows.get(row);
                    for (Map.Entry<Integer, String> column : columns.entrySet()) data.put(column.getValue(), column.getKey() < cells.size() ? mapper.trim(cells.get(column.getKey())) : "");
                    if (!mapper.skip(data.get("POSITION_NAME"), data)) positions.add(mapper.position(data, noticeId, attachment.getAttachmentId(), "ATTACHMENT_DOCX", "table[" + (tableIndex + 1) + "]", row + 1, mapper.raw(data)));
                }
            }
            if (positions.isEmpty()) positions.addAll(textExtractor.extract(paragraphs(document), noticeId, attachment.getAttachmentId(), "ATTACHMENT_DOCX", "paragraph"));
            return new RecruitmentPositionExtractionResult(positions, tables.size(), headers);
        } catch (Exception e) { throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "DOCX岗位附件读取失败：" + e.getMessage()); }
    }
    private List<List<String>> rows(XWPFTable table) { List<List<String>> result = new ArrayList<>(); for (XWPFTableRow row : table.getRows()) { List<String> cells = new ArrayList<>(); for (XWPFTableCell cell : row.getTableCells()) cells.add(cell.getText().trim()); result.add(cells); } return result; }
    private int findHeader(List<List<String>> rows) { for (int row = 0; row < Math.min(60, rows.size()); row++) { Set<String> fields = new HashSet<>(); for (String cell : rows.get(row)) { String field = normalizer.normalize(cell); if (field != null) fields.add(field); } if (fields.contains("POSITION_NAME") && fields.size() >= 3) return row; } return -1; }
    private Map<Integer, String> columns(List<String> row) { Map<Integer, String> result = new LinkedHashMap<>(); for (int index = 0; index < row.size(); index++) { String field = normalizer.normalize(row.get(index)); if (field != null && !result.containsValue(field)) result.put(index, field); } return result; }
    private String paragraphs(XWPFDocument document) { StringBuilder value = new StringBuilder(); for (XWPFParagraph paragraph : document.getParagraphs()) if (!paragraph.getText().trim().isEmpty()) value.append(paragraph.getText()).append('\n'); return value.toString(); }
}
