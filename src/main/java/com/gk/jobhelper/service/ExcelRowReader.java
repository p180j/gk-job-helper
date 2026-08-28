package com.gk.jobhelper.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ExcelRawRow;
import com.gk.jobhelper.dto.ExcelRawSheet;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 全量原始行读取器：
 * 按指定 Sheet 名称（为空时取第一个 Sheet）读取表头与全部数据行，
 * 行号按 Excel 实际行号记录（表头为第 1 行，数据行从 2 起）。
 */
@Component
public class ExcelRowReader {

    public ExcelRawSheet read(File file, String sheetName) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new BusinessException("Excel 文件不存在: " + (file == null ? "null" : file.getPath()));
        }
        RawRowListener listener = new RawRowListener();
        String targetSheet = sheetName == null ? null : sheetName.trim();
        int headerRowIndex = ExcelHeaderDetector.detect(file, targetSheet);
        try {
            if (targetSheet == null || targetSheet.isEmpty()) {
                EasyExcel.read(file, null, listener).headRowNumber(headerRowIndex + 1).sheet(0).doRead();
            } else {
                EasyExcel.read(file, null, listener).headRowNumber(headerRowIndex + 1).sheet(targetSheet).doRead();
            }
        } catch (Exception e) {
            throw new BusinessException("读取 Excel 失败，请确认文件有效且 Sheet 名称["
                    + (targetSheet == null || targetSheet.isEmpty() ? "首个" : targetSheet) + "]存在");
        }
        return listener.toSheet();
    }

    private static class RawRowListener extends AnalysisEventListener<Map<Integer, String>> {

        private final List<String> headers = new ArrayList<>();
        private final List<ExcelRawRow> rows = new ArrayList<>();
        private String sheetName;

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            if (sheetName == null) {
                sheetName = context.readSheetHolder().getSheetName();
            }
            if (headMap == null || headMap.isEmpty()) {
                return;
            }
            // 跳过标题/说明行时只保留最后一次表头回调（实际表头）
            headers.clear();
            List<Integer> indexes = new ArrayList<>(headMap.keySet());
            Collections.sort(indexes);
            for (Integer index : indexes) {
                String header = headMap.get(index);
                headers.add(header == null ? "" : header);
            }
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            if (isBlankRow(data)) {
                return;
            }
            int rowNumber = context.readRowHolder().getRowIndex() + 1;
            rows.add(new ExcelRawRow(rowNumber, new LinkedHashMap<>(data)));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (sheetName == null) {
                sheetName = context.readSheetHolder().getSheetName();
            }
        }

        private boolean isBlankRow(Map<Integer, String> data) {
            if (data == null || data.isEmpty()) {
                return true;
            }
            for (String value : data.values()) {
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        ExcelRawSheet toSheet() {
            ExcelRawSheet sheet = new ExcelRawSheet();
            sheet.setSheetName(sheetName == null ? "" : sheetName);
            sheet.setHeaders(headers);
            sheet.setRows(rows);
            return sheet;
        }
    }
}
