package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 上传预览响应
 */
public class ExcelPreviewVO {

    /** 导入文件记录 id，后续字段映射/导入接口以此引用 */
    private Long fileId;
    /** 原始文件名 */
    private String fileName;
    /** 文件类型 (.xls / .xlsx) */
    private String fileType;
    /** Sheet 名称 */
    private String sheetName;
    /** 表头字段 */
    private List<String> headers = new ArrayList<>();
    /** 数据总行数（不含表头） */
    private Integer totalRows;
    /** 前 10 行数据预览，每行为 表头 -> 值 */
    private List<Map<String, String>> previewRows = new ArrayList<>();

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

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

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public List<Map<String, String>> getPreviewRows() {
        return previewRows;
    }

    public void setPreviewRows(List<Map<String, String>> previewRows) {
        this.previewRows = previewRows;
    }
}
