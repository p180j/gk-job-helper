package com.gk.jobhelper.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Excel 预览解析器：可解析工作簿中每个 Sheet 的表头与数据行，不写任何岗位数据。
 * 保留原始列数据（列下标 -> 值），为后续字段映射（Iteration 2）预留扩展能力。
 */
@Component
public class ExcelPreviewParser {

    static final int DEFAULT_PREVIEW_LIMIT = 10;

    public ExcelPreviewResult parse(File file) {
        return parse(file, null);
    }

    public List<ExcelPreviewResult> parseAll(File file) {
        List<String> sheetNames = listSheetNames(file);
        List<ExcelPreviewResult> results = new ArrayList<>();
        for (String sheetName : sheetNames) {
            results.add(parse(file, sheetName));
        }
        return results;
    }

    private ExcelPreviewResult parse(File file, String requestedSheetName) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new BusinessException("待解析的 Excel 文件不存在");
        }
        PreviewReadListener listener = new PreviewReadListener(DEFAULT_PREVIEW_LIMIT);
        try {
            int headerRowIndex = ExcelHeaderDetector.detect(file, requestedSheetName);
            // 无模型读取：自动跳过标题/说明行，实际表头触发 invokeHeadMap
            if (requestedSheetName == null || requestedSheetName.trim().isEmpty()) {
                EasyExcel.read(file, null, listener).headRowNumber(headerRowIndex + 1).sheet(0).doRead();
            } else {
                EasyExcel.read(file, null, listener).headRowNumber(headerRowIndex + 1).sheet(requestedSheetName).doRead();
            }
        } catch (Exception e) {
            throw new BusinessException("Excel 文件解析失败，请确认文件是有效的 .xls / .xlsx 文件");
        }
        return listener.toResult();
    }

    private List<String> listSheetNames(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new BusinessException("待解析的 Excel 文件不存在");
        }
        try (FileInputStream input = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetName(i));
            }
            if (names.isEmpty()) throw new BusinessException("Excel 文件不包含可读取的 Sheet");
            return names;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Excel 文件解析失败，请确认文件是有效的 .xls / .xlsx 文件");
        }
    }

    private static class PreviewReadListener extends AnalysisEventListener<Map<Integer, String>> {

        private final int previewLimit;
        private final List<String> headers = new ArrayList<>();
        private final List<Map<Integer, String>> previewRows = new ArrayList<>();
        private int totalRows = 0;
        private String sheetName;

        PreviewReadListener(int previewLimit) {
            this.previewLimit = previewLimit;
        }

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            if (sheetName == null) {
                sheetName = context.readSheetHolder().getSheetName();
            }
            if (headMap == null || headMap.isEmpty()) {
                return;
            }
            // headRowNumber 大于 1 时会依次回调标题行；仅保留最后一次（实际表头）
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
            totalRows++;
            if (data != null && previewRows.size() < previewLimit) {
                previewRows.add(new LinkedHashMap<>(data));
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (sheetName == null) {
                sheetName = context.readSheetHolder().getSheetName();
            }
        }

        ExcelPreviewResult toResult() {
            ExcelPreviewResult result = new ExcelPreviewResult();
            result.setSheetName(sheetName == null ? "" : sheetName);
            result.setHeaders(headers);
            result.setTotalRows(totalRows);
            result.setPreviewRows(previewRows);
            return result;
        }
    }
}
