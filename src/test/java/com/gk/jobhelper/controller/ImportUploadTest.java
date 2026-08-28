package com.gk.jobhelper.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel 上传预览 API 集成测试（H2 内存库）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportUploadTest {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportFileMapper importFileMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        importFileMapper.deleteAll();
    }

    @Test
    void uploadXlsxShouldReturnPreviewWithFirstTenRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "岗位表.xlsx", XLSX_CONTENT_TYPE, buildExcel(ExcelTypeEnum.XLSX, 12));

        JsonNode data = performUpload(file);

        assertEquals("岗位表.xlsx", data.get("fileName").asText());
        assertEquals(".xlsx", data.get("fileType").asText());
        assertEquals("职位表", data.get("sheetName").asText());
        assertEquals(3, data.get("headers").size());
        assertEquals("部门", data.get("headers").get(0).asText());
        assertEquals("职位名称", data.get("headers").get(1).asText());
        assertEquals("招考人数", data.get("headers").get(2).asText());
        assertEquals(12, data.get("totalRows").asInt());
        assertEquals(10, data.get("previewRows").size());
        assertEquals("部门1", data.get("previewRows").get(0).get("部门").asText());
        assertEquals("职位10", data.get("previewRows").get(9).get("职位名称").asText());
        assertTrue(data.get("fileId").asLong() > 0);

        // 数据库记录与本地文件保存校验
        ImportFile record = importFileMapper.selectLatest();
        assertNotNull(record);
        assertEquals("岗位表.xlsx", record.getOriginalName());
        assertEquals("职位表", record.getSheetName());
        assertEquals("部门,职位名称,招考人数", record.getHeaders());
        assertEquals(12, record.getTotalRows().intValue());
        assertEquals("PREVIEWED", record.getStatus());
        assertTrue(Files.exists(Paths.get(record.getStoredPath())), "原始文件应保存到本地 uploads 目录");
    }

    @Test
    void uploadXlsShouldReturnPreview() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "岗位表.xls", "application/vnd.ms-excel", buildExcel(ExcelTypeEnum.XLS, 3));

        JsonNode data = performUpload(file);

        assertEquals("岗位表.xls", data.get("fileName").asText());
        assertEquals(".xls", data.get("fileType").asText());
        assertEquals("职位表", data.get("sheetName").asText());
        assertEquals(3, data.get("totalRows").asInt());
        assertEquals(3, data.get("previewRows").size());
        assertEquals("部门1", data.get("previewRows").get(0).get("部门").asText());
    }

    @Test
    void uploadUnsupportedExtensionShouldBeRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "岗位表.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/import/upload").file(file))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(40000, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("仅支持"));
    }

    @Test
    void uploadInvalidExcelContentShouldBeRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.xlsx", XLSX_CONTENT_TYPE, "not an excel".getBytes(StandardCharsets.UTF_8));

        MvcResult result = mockMvc.perform(multipart("/api/import/upload").file(file))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(40000, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("解析失败"));
    }

    private JsonNode performUpload(MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/import/upload").file(file))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private String content(MvcResult result) throws java.io.UnsupportedEncodingException {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 生成测试用 Excel 字节流: 表头行 + dataRows 行数据
     */
    private byte[] buildExcel(ExcelTypeEnum type, int dataRows) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("部门", "职位名称", "招考人数"));
        for (int i = 1; i <= dataRows; i++) {
            rows.add(Arrays.asList("部门" + i, "职位" + i, String.valueOf(i)));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).excelType(type).sheet("职位表").doWrite(rows);
        return out.toByteArray();
    }
}
