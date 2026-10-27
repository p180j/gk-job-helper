package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.entity.JobPosition;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelMultiTemplateTest {
    @TempDir File tempDir;
    private final ExcelRowReader reader = new ExcelRowReader();
    private final FieldMappingService mappingService = new FieldMappingService();
    private final JobPositionConverter converter = new JobPositionConverter(new ObjectMapper(), mappingService);

    @Test
    void differentSheetHeadersAndOrdersKeepEquivalentQualificationFields() throws Exception {
        File file = new File(tempDir, "variants.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet first = workbook.createSheet("第一类");
            row(first, 0, "说明：以下为招考职位");
            row(first, 1, "招录机关", "职位名称", "职位代码", "其他条件", "备注", "工作地点", "落户地点");
            row(first, 2, "甲机关", "软件岗", "00123", "须取得证书A", "最低服务3年", "南昌市", "九江市", "无表头附加值");
            Sheet second = workbook.createSheet("第二类");
            row(second, 0, "无关说明");
            row(second, 1, "落户地", "岗位代码", "备注信息", "岗位名称", "部门名称", "实际工作地点", "其他要求", "未知列");
            row(second, 2, "九江市", "00123", "最低服务3年", "软件岗", "乙机关", "南昌市", "须取得证书A", "保留原文");
            workbook.createSheet("说明Sheet");
            try (FileOutputStream out = new FileOutputStream(file)) { workbook.write(out); }
        }
        ExcelRawSheet first = reader.read(file, "第一类");
        ExcelRawSheet second = reader.read(file, "第二类");
        ImportConfirmRequest request = new ImportConfirmRequest();
        request.setSheetNames(Arrays.asList("第一类", "第二类"));
        request.setMappings(suggested(first));
        ConversionResult firstRows = converter.convert(first, mappingService.mappingsForSheet(first, request), 1L);
        ConversionResult secondRows = converter.convert(second, mappingService.mappingsForSheet(second, request), 1L);
        assertEquals(1, firstRows.getPositions().size());
        assertEquals(1, secondRows.getPositions().size());
        JobPosition a = firstRows.getPositions().get(0), b = secondRows.getPositions().get(0);
        assertEquals("00123", a.getPositionCode());
        assertEquals(a.getPositionCode(), b.getPositionCode());
        assertEquals(a.getAdditionalRequirements(), b.getAdditionalRequirements());
        assertEquals(a.getRemark(), b.getRemark());
        assertEquals(a.getWorkLocation(), b.getWorkLocation());
        assertEquals(a.getSettlementLocation(), b.getSettlementLocation());
        assertEquals("甲机关", a.getDepartmentName());
        assertTrue(a.getRawData().contains("无表头附加值"));
        assertEquals("乙机关", b.getDepartmentName());
        assertEquals(3, b.getSourceRow().intValue());
        assertTrue(b.getRawData().contains("未知列"));
        assertTrue(new ExcelPreviewParser().parseAll(file).get(2).getHeaders().isEmpty());
    }

    @Test
    void formattedCodeKeepsLeadingZeroAndUpperHeaderRepairsDamagedCell() throws Exception {
        File file = new File(tempDir, "formatted.xls");
        try (Workbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("职位");
            row(sheet, 0, "", "", "", "招考人数");
            row(sheet, 1, "部门名称", "招考职位", "职位代码", "4");
            Row data = sheet.createRow(2);
            data.createCell(0).setCellValue("甲机关");
            data.createCell(1).setCellValue("管理岗");
            CellStyle codeStyle = workbook.createCellStyle();
            codeStyle.setDataFormat(workbook.createDataFormat().getFormat("000000"));
            data.createCell(2).setCellValue(123);
            data.getCell(2).setCellStyle(codeStyle);
            data.createCell(3).setCellValue(2);
            try (FileOutputStream out = new FileOutputStream(file)) { workbook.write(out); }
        }
        ExcelRawSheet sheet = reader.read(file, "职位");
        assertEquals("招考人数", sheet.getHeaders().get(3));
        assertEquals("000123", sheet.getRows().get(0).getCells().get(2));
        JobPosition position = converter.convert(sheet, suggested(sheet), 1L).getPositions().get(0);
        assertEquals("000123", position.getPositionCode());
        assertEquals(Integer.valueOf(2), position.getRecruitCount());
        assertNull(position.getWorkLocation());
    }

    private List<ImportConfirmRequest.MappingItem> suggested(ExcelRawSheet sheet) {
        List<ImportConfirmRequest.MappingItem> result = new ArrayList<>();
        for (String header : sheet.getHeaders()) {
            ImportConfirmRequest.MappingItem item = new ImportConfirmRequest.MappingItem();
            item.setSourceField(header);
            item.setTargetField(mappingService.suggest(header).getSuggestedField());
            result.add(item);
        }
        return result;
    }

    @Test
    void duplicatedHeadersHaveDistinctMappingsAndKeepBothRestrictions() throws Exception {
        File file = new File(tempDir, "duplicates.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("岗位");
            row(sheet, 0, "招录机关", "职位名称", "职位代码", "其他条件", "其他条件");
            row(sheet, 1, "某机关", "某岗位", "001", "限男性", "须取得教师资格证");
            try (FileOutputStream out = new FileOutputStream(file)) { workbook.write(out); }
        }
        ExcelRawSheet sheet = reader.read(file, "岗位");
        assertEquals("其他条件(2)", sheet.getHeaders().get(4));
        List<ImportConfirmRequest.MappingItem> mappings = suggested(sheet);
        mappings.get(4).setTargetField("additionalRequirements");
        JobPosition position = converter.convert(sheet, mappings, 1L).getPositions().get(0);
        assertEquals("限男性；须取得教师资格证", position.getAdditionalRequirements());
    }

    private void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
    }
}
