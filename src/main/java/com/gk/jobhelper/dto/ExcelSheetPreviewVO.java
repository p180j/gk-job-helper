package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 单个 Excel Sheet 的预览与导入建议。 */
public class ExcelSheetPreviewVO {
    private String sheetName;
    private List<String> headers = new ArrayList<>();
    private Integer totalRows;
    private List<Map<String, String>> previewRows = new ArrayList<>();
    private boolean suggestedForImport;

    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public List<Map<String, String>> getPreviewRows() { return previewRows; }
    public void setPreviewRows(List<Map<String, String>> previewRows) { this.previewRows = previewRows; }
    public boolean isSuggestedForImport() { return suggestedForImport; }
    public void setSuggestedForImport(boolean suggestedForImport) { this.suggestedForImport = suggestedForImport; }
}
