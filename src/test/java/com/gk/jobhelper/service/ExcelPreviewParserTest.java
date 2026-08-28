package com.gk.jobhelper.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Excel 预览解析器单元测试（纯 JUnit，不依赖数据库）
 */
class ExcelPreviewParserTest {

    private ExcelPreviewParser parser;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        parser = new ExcelPreviewParser();
    }

    @Test
    void parseXlsxShouldReturnHeadersAndRows() throws IOException {
        File file = new File(tempDir, "positions.xlsx");
        writeExcel(file, ExcelTypeEnum.XLSX, 3);

        ExcelPreviewResult result = parser.parse(file);

        assertEquals("职位表", result.getSheetName());
        assertEquals(Arrays.asList("部门", "职位名称", "招考人数"), result.getHeaders());
        assertEquals(3, result.getTotalRows());
        assertEquals(3, result.getPreviewRows().size());
        assertEquals("部门1", result.getPreviewRows().get(0).get(0));
        assertEquals("职位1", result.getPreviewRows().get(0).get(1));
        assertEquals("3", result.getPreviewRows().get(2).get(2));
    }

    @Test
    void parseXlsShouldReturnHeadersAndRows() throws IOException {
        File file = new File(tempDir, "positions.xls");
        writeExcel(file, ExcelTypeEnum.XLS, 2);

        ExcelPreviewResult result = parser.parse(file);

        assertEquals("职位表", result.getSheetName());
        assertEquals(Arrays.asList("部门", "职位名称", "招考人数"), result.getHeaders());
        assertEquals(2, result.getTotalRows());
        assertEquals("部门1", result.getPreviewRows().get(0).get(0));
    }

    @Test
    void parseHeaderOnlyFileShouldReturnZeroRows() throws IOException {
        File file = new File(tempDir, "header-only.xlsx");
        writeExcel(file, ExcelTypeEnum.XLSX, 0);

        ExcelPreviewResult result = parser.parse(file);

        assertEquals(Arrays.asList("部门", "职位名称", "招考人数"), result.getHeaders());
        assertEquals(0, result.getTotalRows());
        assertTrue(result.getPreviewRows().isEmpty());
    }

    @Test
    void parseMoreThanTenRowsShouldLimitPreviewToTen() throws IOException {
        File file = new File(tempDir, "many-rows.xlsx");
        writeExcel(file, ExcelTypeEnum.XLSX, 15);

        ExcelPreviewResult result = parser.parse(file);

        assertEquals(15, result.getTotalRows());
        assertEquals(10, result.getPreviewRows().size());
        assertEquals("部门10", result.getPreviewRows().get(9).get(0));
    }

    @Test
    void parseShouldSkipTitleRowsBeforeActualHeader() throws IOException {
        File file = new File(tempDir, "official-style.xlsx");
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("附件1"));
        rows.add(Arrays.asList("江西省公务员职位表"));
        rows.add(Arrays.asList("职位层级", "单位名称", "职位名称", "职位代码", "招考人数"));
        rows.add(Arrays.asList("省级", "测试单位", "软件工程岗", "3601001", "2"));
        EasyExcel.write(file).sheet("职位表").doWrite(rows);

        ExcelPreviewResult result = parser.parse(file);

        assertEquals(Arrays.asList("职位层级", "单位名称", "职位名称", "职位代码", "招考人数"),
                result.getHeaders());
        assertEquals(1, result.getTotalRows());
        assertEquals("软件工程岗", result.getPreviewRows().get(0).get(2));
    }

    @Test
    void parseNonExcelFileShouldThrowBusinessException() throws IOException {
        File file = new File(tempDir, "fake.xlsx");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("this is not an excel file".getBytes(StandardCharsets.UTF_8));
        }

        BusinessException exception = assertThrows(BusinessException.class, () -> parser.parse(file));
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("解析失败"));
    }

    @Test
    void parseMissingFileShouldThrowBusinessException() {
        File file = new File(tempDir, "not-exist.xlsx");
        assertThrows(BusinessException.class, () -> parser.parse(file));
    }

    /**
     * 生成测试用 Excel: 表头行 + dataRows 行数据，全部为文本单元格
     */
    private void writeExcel(File file, ExcelTypeEnum type, int dataRows) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("部门", "职位名称", "招考人数"));
        for (int i = 1; i <= dataRows; i++) {
            rows.add(Arrays.asList("部门" + i, "职位" + i, String.valueOf(i)));
        }
        EasyExcel.write(file).excelType(type).sheet("职位表").doWrite(rows);
    }
}
