package com.gk.jobhelper.dto;

import com.gk.jobhelper.entity.JobPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * 行转换结果（内部使用）：成功转换的岗位列表 + 失败行明细
 */
public class ConversionResult {

    private List<JobPosition> positions = new ArrayList<>();
    private List<ImportFailedItem> failures = new ArrayList<>();

    public List<JobPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<JobPosition> positions) {
        this.positions = positions;
    }

    public List<ImportFailedItem> getFailures() {
        return failures;
    }

    public void setFailures(List<ImportFailedItem> failures) {
        this.failures = failures;
    }
}
