package com.gk.jobhelper.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.constant.PositionStandardField;
import com.gk.jobhelper.dto.ExcelRawRow;
import com.gk.jobhelper.dto.ExcelRawSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
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
        String targetSheet = sheetName == null ? null : sheetName.trim();
        try {
            RawRowListener listener = new RawRowListener();
            int headerRowIndex = ExcelHeaderDetector.detect(file, targetSheet);
            if (targetSheet == null || targetSheet.isEmpty()) {
                EasyExcel.read(file, null, listener).headRowNumber(headerRowIndex + 1).sheet(0).doRead();
            } else {
                EasyExcel.read(file, null, listener).headRowNumber(headerRowIndex + 1).sheet(targetSheet).doRead();
            }
            return listener.toSheet();
        } catch (Exception e) {
            return readWithPoi(file, targetSheet);
        }
    }

    private ExcelRawSheet readWithPoi(File file, String sheetName) {
        try (FileInputStream input = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = sheetName == null || sheetName.isEmpty() ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Sheet 不存在: " + sheetName);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int headerIndex = detectHeaderRow(sheet, formatter, evaluator);
            Row headerRow = sheet.getRow(headerIndex);
            int columnCount = headerRow == null ? 0 : Math.max(0, headerRow.getLastCellNum());
            if (columnCount == 0) throw new IllegalArgumentException("未找到有效表头");
            ExcelRawSheet result = new ExcelRawSheet();
            result.setSheetName(sheet.getSheetName());
            List<String> headers = new ArrayList<>();
            for (int column = 0; column < columnCount; column++) headers.add(text(headerRow.getCell(column), formatter, evaluator));
            result.setHeaders(headers);
            List<ExcelRawRow> rows = new ArrayList<>();
            for (int rowIndex = headerIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                Map<Integer, String> cells = new LinkedHashMap<>();
                boolean nonBlank = false;
                for (int column = 0; column < columnCount; column++) {
                    String value = text(row.getCell(column), formatter, evaluator);
                    cells.put(column, value);
                    if (!value.isEmpty()) nonBlank = true;
                }
                if (nonBlank) rows.add(new ExcelRawRow(rowIndex + 1, cells));
            }
            result.setRows(rows);
            return result;
        } catch (Exception e) {
            String detail = e.getMessage();
            if (detail == null || detail.trim().isEmpty()) detail = e.getClass().getSimpleName();
            throw new BusinessException("读取 Excel 失败，请确认文件是有效的 .xls/.xlsx 文件（" + detail + "）");
        }
    }

    private int detectHeaderRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int bestIndex = 0, bestScore = 0;
        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 29); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            int nonBlank = 0, score = 0;
            for (Cell cell : row) {
                String normalized = PositionStandardField.normalize(text(cell, formatter, evaluator));
                if (normalized.isEmpty()) continue;
                nonBlank++;
                boolean recognized = false;
                for (PositionStandardField field : PositionStandardField.values()) {
                    if (field.matchesExact(normalized) || field.matchesAlias(normalized)) { recognized = true; break; }
                }
                if (recognized || InterviewScoreHeaderPolicy.isScore(normalized)) score++;
            }
            if (nonBlank >= 3 && score >= 2 && score > bestScore) { bestIndex = rowIndex; bestScore = score; }
        }
        return bestIndex;
    }

    private String text(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try { return formatter.formatCellValue(cell, evaluator).trim(); }
        catch (Exception ignored) { return formatter.formatCellValue(cell).trim(); }
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
