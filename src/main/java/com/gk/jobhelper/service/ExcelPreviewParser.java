package com.gk.jobhelper.service;

import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import com.gk.jobhelper.dto.ExcelRawRow;
import com.gk.jobhelper.dto.ExcelRawSheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Uses the import reader for previews so sheet selection and saved rows share the same header semantics. */
@Component
public class ExcelPreviewParser {
    static final int DEFAULT_PREVIEW_LIMIT = 10;
    private final ExcelRowReader rowReader = new ExcelRowReader();

    public ExcelPreviewResult parse(File file) {
        List<ExcelPreviewResult> sheets = parseAll(file);
        for (ExcelPreviewResult sheet : sheets) if (!sheet.getHeaders().isEmpty()) return sheet;
        throw new BusinessException("Excel 文件没有可导入的职位 Sheet");
    }

    public List<ExcelPreviewResult> parseAll(File file) {
        if (file == null || !file.isFile()) throw new BusinessException("待解析的 Excel 文件不存在");
        List<String> names = listSheetNames(file);
        List<ExcelPreviewResult> results = new ArrayList<>();
        for (String name : names) {
            ExcelPreviewResult result = new ExcelPreviewResult();
            result.setSheetName(name);
            try {
                ExcelRawSheet sheet = rowReader.read(file, name);
                result.setHeaders(sheet.getHeaders());
                result.setTotalRows(sheet.getRows().size());
                List<java.util.Map<Integer, String>> preview = new ArrayList<>();
                for (int i = 0; i < Math.min(DEFAULT_PREVIEW_LIMIT, sheet.getRows().size()); i++) {
                    ExcelRawRow row = sheet.getRows().get(i);
                    preview.add(new LinkedHashMap<>(row.getCells()));
                }
                result.setPreviewRows(preview);
            } catch (BusinessException e) {
                result.setParseError(e.getMessage());
            }
            results.add(result);
        }
        return results;
    }

    private List<String> listSheetNames(File file) {
        try (FileInputStream input = new FileInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            List<String> names = new ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                names.add(workbook.getSheetName(index));
            }
            if (names.isEmpty()) throw new BusinessException("Excel 文件不包含 Sheet");
            return names;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Excel 文件解析失败，请确认文件是有效的 .xls / .xlsx 文件");
        }
    }
}
