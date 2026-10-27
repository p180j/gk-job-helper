package com.gk.jobhelper.service;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads source cells as displayed text, retaining formatted leading zeroes and Excel row numbers. */
@Component
public class ExcelRowReader {
    private static final int HEADER_SCAN_LIMIT = 30;
    private static final int MIN_HEADER_SCORE = 2;

    public ExcelRawSheet read(File file, String sheetName) {
        if (file == null || !file.isFile()) throw new BusinessException("Excel 文件不存在");
        try (FileInputStream input = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = sheetName == null || sheetName.trim().isEmpty()
                    ? workbook.getSheetAt(0) : workbook.getSheet(sheetName.trim());
            if (sheet == null) throw new BusinessException("Sheet 不存在: " + sheetName);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            int headerIndex = detectHeaderRow(sheet, formatter, evaluator);
            Row headerRow = sheet.getRow(headerIndex);
            if (headerRow == null) throw new BusinessException("未找到有效表头: " + sheet.getSheetName());
            int columnCount = Math.max(0, headerRow.getLastCellNum());
            for (int rowIndex = headerIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row data = sheet.getRow(rowIndex);
                if (data == null) continue;
                for (Cell cell : data) {
                    if (!text(cell, formatter, evaluator).isEmpty()) columnCount = Math.max(columnCount, cell.getColumnIndex() + 1);
                }
            }
            if (columnCount == 0) throw new BusinessException("未找到有效表头: " + sheet.getSheetName());
            List<String> headers = headers(sheet, headerIndex, columnCount, formatter, evaluator);
            ExcelRawSheet result = new ExcelRawSheet();
            result.setSheetName(sheet.getSheetName());
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
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("读取 Excel 失败，请确认文件是有效的 .xls/.xlsx 文件（" + e.getMessage() + "）");
        }
    }

    private int detectHeaderRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int bestIndex = -1, bestScore = 0;
        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), HEADER_SCAN_LIMIT - 1); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            int recognized = 0, nonBlank = 0;
            boolean interviewScoreHeader = false;
            for (Cell cell : row) {
                String value = text(cell, formatter, evaluator);
                if (value.isEmpty()) continue;
                nonBlank++;
                if (knownHeader(value)) recognized++;
                if (isInterviewScoreHeader(value)) interviewScoreHeader = true;
            }
            if ((nonBlank >= 3 && recognized >= MIN_HEADER_SCORE
                    || nonBlank >= 2 && interviewScoreHeader && recognized >= 1)
                    && recognized > bestScore) {
                bestIndex = rowIndex;
                bestScore = recognized;
            }
        }
        if (bestIndex < 0) throw new BusinessException("未找到可靠的职位表头: " + sheet.getSheetName());
        return bestIndex;
    }

    private List<String> headers(Sheet sheet, int rowIndex, int count,
                                 DataFormatter formatter, FormulaEvaluator evaluator) {
        Row current = sheet.getRow(rowIndex);
        Row previous = rowIndex == 0 ? null : sheet.getRow(rowIndex - 1);
        List<String> values = new ArrayList<>();
        java.util.Set<String> used = new java.util.HashSet<>();
        for (int column = 0; column < count; column++) {
            String value = text(current.getCell(column), formatter, evaluator);
            String parent = previous == null ? "" : text(previous.getCell(column), formatter, evaluator);
            // A merged or damaged lower header cell can retain a number. Use the recognized parent only then.
            if (!knownHeader(value) && knownHeader(parent) && (value.isEmpty() || value.matches("\\d+"))) {
                value = parent;
            }
            String original = value.isEmpty() ? "列" + (column + 1) : value;
            String unique = original;
            int suffix = 2;
            while (!used.add(PositionStandardField.normalize(unique))) unique = original + "(" + suffix++ + ")";
            values.add(unique);
        }
        return values;
    }

    private boolean knownHeader(String value) {
        if (isInterviewScoreHeader(value)) return true;
        String normalized = PositionStandardField.normalize(value);
        for (PositionStandardField field : PositionStandardField.values()) {
            if (field.matchesExact(normalized) || field.matchesAlias(normalized)) return true;
        }
        return false;
    }

    private boolean isInterviewScoreHeader(String value) {
        String compact = value.replaceAll("\\s+", "");
        return compact.contains("面试") && (compact.contains("最低分") || compact.contains("成绩"))
                || compact.contains("笔试总成绩");
    }

    private String text(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try { return formatter.formatCellValue(cell, evaluator).trim(); }
        catch (Exception ignored) { return formatter.formatCellValue(cell).trim(); }
    }
}
