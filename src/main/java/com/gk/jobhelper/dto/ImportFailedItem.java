package com.gk.jobhelper.dto;

/**
 * 导入失败的行信息
 */
public class ImportFailedItem {

    /** Excel 实际行号（表头为第 1 行，数据行从 2 起） */
    private Integer row;
    /** 失败原因 */
    private String reason;

    public ImportFailedItem() {
    }

    public ImportFailedItem(Integer row, String reason) {
        this.row = row;
        this.reason = reason;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
