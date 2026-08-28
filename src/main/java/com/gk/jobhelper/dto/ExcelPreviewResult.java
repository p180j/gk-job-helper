package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 解析原始结果（内部使用）
 * previewRows 保留"列下标 -> 值"的原始结构，为后续字段映射预留扩展能力
 */
public class ExcelPreviewResult {

    private String sheetName;
    private List<String> headers = new ArrayList<>();
    private int totalRows;
    private List<Map<Integer, String>> previewRows = new ArrayList<>();

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public List<Map<Integer, String>> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<Map<Integer, String>> previewRows) {
        this.previewRows = previewRows;
    }
}
