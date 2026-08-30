package com.gk.jobhelper.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.constant.PositionStandardField;

import java.io.File;
import java.util.Map;

/**
 * 识别职位表的实际表头行。
 * 官方职位表通常在表头前包含附件编号、标题或说明；仅在前 30 行内按既有字段字典寻找
 * 至少两个可识别字段的行，避免把标题行误判为表头。普通单表头 Excel 仍会识别第 1 行。
 */
final class ExcelHeaderDetector {

    private static final int SCAN_ROW_LIMIT = 30;
    private static final int MIN_RECOGNIZED_HEADERS = 2;

    private ExcelHeaderDetector() {
    }

    static int detect(File file, String sheetName) {
        HeaderScanListener listener = new HeaderScanListener();
        try {
            if (sheetName == null || sheetName.trim().isEmpty()) {
                EasyExcel.read(file, null, listener).headRowNumber(0).sheet(0).doRead();
            } else {
                EasyExcel.read(file, null, listener).headRowNumber(0).sheet(sheetName.trim()).doRead();
            }
        } catch (Exception e) {
            throw new BusinessException("读取 Excel 表头失败，请确认文件和 Sheet 有效");
        }
        return listener.getHeaderRowIndex();
    }

    private static class HeaderScanListener extends AnalysisEventListener<Map<Integer, String>> {

        private int headerRowIndex = 0;
        private int bestScore = 0;

        @Override
        public void invoke(Map<Integer, String> row, AnalysisContext context) {
            int rowIndex = context.readRowHolder().getRowIndex();
            if (rowIndex >= SCAN_ROW_LIMIT || row == null || row.isEmpty()) {
                return;
            }
            int nonBlank = 0;
            int score = 0;
            for (String value : row.values()) {
                String normalized = PositionStandardField.normalize(value);
                if (normalized.isEmpty()) {
                    continue;
                }
                nonBlank++;
                for (PositionStandardField field : PositionStandardField.values()) {
                    if (field.matchesExact(normalized) || field.matchesAlias(normalized)) {
                        score++;
                        break;
                    }
                }
                if (InterviewScoreHeaderPolicy.isScore(normalized)) {
                    score++;
                }
            }
            if (nonBlank >= 3 && score >= MIN_RECOGNIZED_HEADERS && score > bestScore) {
                headerRowIndex = rowIndex;
                bestScore = score;
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 无需处理
        }

        int getHeaderRowIndex() {
            return headerRowIndex;
        }

    }
}
