package com.gk.jobhelper.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excel 原始数据行（内部使用）
 */
public class ExcelRawRow {

    /** Excel 实际行号：表头为第 1 行，数据行从 2 开始 */
    private int rowNumber;
    /** 列下标 -> 原始单元格值 */
    private Map<Integer, String> cells = new LinkedHashMap<>();

    public ExcelRawRow() {
    }

    public ExcelRawRow(int rowNumber, Map<Integer, String> cells) {
        this.rowNumber = rowNumber;
        this.cells = cells == null ? new LinkedHashMap<>() : cells;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Map<Integer, String> getCells() {
        return cells;
    }

    public void setCells(Map<Integer, String> cells) {
        this.cells = cells;
    }
}
