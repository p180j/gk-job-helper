package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段映射预览响应（GET /api/import/{id}/mapping）
 */
public class FieldMappingPreviewVO {

    private Long importId;
    private String sheetName;
    private List<ExcelSheetPreviewVO> sheets = new ArrayList<>();
    private List<HeaderSuggestion> headers = new ArrayList<>();

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<ExcelSheetPreviewVO> getSheets() { return sheets; }
    public void setSheets(List<ExcelSheetPreviewVO> sheets) { this.sheets = sheets; }

    public List<HeaderSuggestion> getHeaders() {
        return headers;
    }

    public void setHeaders(List<HeaderSuggestion> headers) {
        this.headers = headers;
    }
}
