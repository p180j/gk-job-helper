package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 单个 Sheet 的全量原始数据（内部使用）
 */
public class ExcelRawSheet {

    private String sheetName;
    /** 表头（按列顺序，未归一化的原始文本） */
    private List<String> headers = new ArrayList<>();
    private List<ExcelRawRow> rows = new ArrayList<>();

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

    public List<ExcelRawRow> getRows() {
        return rows;
    }

    public void setRows(List<ExcelRawRow> rows) {
        this.rows = rows;
    }
}
