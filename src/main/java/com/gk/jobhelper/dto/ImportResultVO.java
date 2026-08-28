package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 正式导入结果（POST /api/import/{id}/confirm）
 */
public class ImportResultVO {

    private Long importId;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private List<ImportFailedItem> failedItems = new ArrayList<>();

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getSuccessRows() {
        return successRows;
    }

    public void setSuccessRows(Integer successRows) {
        this.successRows = successRows;
    }

    public Integer getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(Integer failedRows) {
        this.failedRows = failedRows;
    }

    public List<ImportFailedItem> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<ImportFailedItem> failedItems) {
        this.failedItems = failedItems;
    }
}
