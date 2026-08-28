package com.gk.jobhelper.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel 字段映射 + 正式导入 + 岗位查询全流程集成测试（H2 内存库）
 * 覆盖:
 * - 映射预览接口(EXACT/ALIAS/UNKNOWN 建议)
 * - 用户人工修改映射后 confirm 导入
 * - raw_data 保存原始整行 JSON(未映射列不丢失)
 * - source_sheet / source_row 记录
 * - 数字字段解析失败容错("若干" -> null, 行不失败)
 * - 批量导入部分行失败(职位代码为空)只跳过该行
 * - 岗位分页查询 / 详情查询(含 rawData)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobImportFlowTest {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String SHEET_NAME = "职位表";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportFileMapper importFileMapper;

    @Autowired
    private JobPositionMapper jobPositionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        jobPositionMapper.deleteAll();
        importFileMapper.deleteAll();
    }

    @Test
    void mappingPreviewShouldReturnSuggestionsWithConfidence() throws Exception {
        long importId = uploadImportFile();

        JsonNode data = getJson("/api/import/" + importId + "/mapping");

        assertEquals(importId, data.get("importId").asLong());
        assertEquals(SHEET_NAME, data.get("sheetName").asText());

        JsonNode headers = data.get("headers");
        assertEquals(5, headers.size());

        // 精确命中 -> EXACT
        assertHeader(headers.get(0), "招录机关", "departmentName", "EXACT");
        assertHeader(headers.get(2), "职位代码", "positionCode", "EXACT");
        // 同义词 -> ALIAS
        assertHeader(headers.get(1), "岗位名称", "positionName", "ALIAS");
        assertHeader(headers.get(3), "招聘人数", "recruitCount", "ALIAS");
        // 无法识别 -> UNKNOWN, 不做猜测
        assertHeader(headers.get(4), "薪资待遇", null, "UNKNOWN");
    }

    @Test
    void mappingPreviewShouldRejectUnknownImportId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/import/999999/mapping"))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(40403, root.get("code").asInt());
    }

    @Test
    void confirmShouldImportWithUserModifiedMappingAndRowLevelTolerance() throws Exception {
        long importId = uploadImportFile();

        // 用户人工修改映射: 未识别列"薪资待遇"人工指定为备注
        JsonNode data = postConfirm(importId, defaultUserMappings());

        // 3 行数据: 第 4 行职位代码为空 -> 失败; 其余 2 行成功
        assertEquals(importId, data.get("importId").asLong());
        assertEquals(3, data.get("totalRows").asInt());
        assertEquals(2, data.get("successRows").asInt());
        assertEquals(1, data.get("failedRows").asInt());
        JsonNode failed = data.get("failedItems").get(0);
        assertEquals(4, failed.get("row").asInt());
        assertTrue(failed.get("reason").asText().contains("职位代码为空"),
                "失败原因应包含职位代码为空: " + failed.get("reason").asText());

        // 落库校验
        List<JobPosition> positions = jobPositionMapper.selectByImportFileId(importId);
        assertEquals(2, positions.size());

        JobPosition first = positions.get(0);
        assertEquals("职位表", first.getSourceSheet());
        assertEquals(2, first.getSourceRow().intValue());
        assertEquals("部门A", first.getDepartmentName());
        assertEquals("Java开发工程师", first.getPositionName());
        assertEquals("3001001", first.getPositionCode());
        assertEquals(Integer.valueOf(3), first.getRecruitCount());
        assertEquals("10000", first.getRemark()); // 人工映射的未识别列
        assertNotNull(first.getCreatedAt());

        JobPosition second = positions.get(1);
        assertEquals(3, second.getSourceRow().intValue());
        assertEquals("数据分析师", second.getPositionName());
        assertEquals("3001002", second.getPositionCode());
        // "若干"无法解析为数字 -> 置 null, 该行不失败
        assertNull(second.getRecruitCount());
        assertEquals("8000", second.getRemark());

        // raw_data 保留该行完整 JSON(含未映射列)
        JsonNode raw = objectMapper.readTree(first.getRawData());
        assertEquals("部门A", raw.get("招录机关").asText());
        assertEquals("Java开发工程师", raw.get("岗位名称").asText());
        assertEquals("3001001", raw.get("职位代码").asText());
        assertEquals("3", raw.get("招聘人数").asText());
        assertEquals("10000", raw.get("薪资待遇").asText());

        // 导入记录状态更新为 IMPORTED
        assertEquals("IMPORTED", importFileMapper.selectById(importId).getStatus());
    }

    @Test
    void confirmTwiceShouldBeIdempotent() throws Exception {
        long importId = uploadImportFile();

        postConfirm(importId, defaultUserMappings());
        JsonNode second = postConfirm(importId, defaultUserMappings());

        assertEquals(2, second.get("successRows").asInt());
        assertEquals(2, jobPositionMapper.selectByImportFileId(importId).size());
    }

    @Test
    void confirmShouldRejectUnknownSourceField() throws Exception {
        long importId = uploadImportFile();

        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(mapping("招录机关", "departmentName"));
        mappings.add(mapping("表里没有的列", "positionName"));

        JsonNode root = postConfirmRaw(importId, mappings);
        assertEquals(40000, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("表头中不存在字段"));
        assertTrue(jobPositionMapper.selectByImportFileId(importId).isEmpty());
    }

    @Test
    void confirmShouldRejectIllegalTargetField() throws Exception {
        long importId = uploadImportFile();

        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(mapping("招录机关", "departmentName"));
        mappings.add(mapping("岗位名称", "notAStandardField"));

        JsonNode root = postConfirmRaw(importId, mappings);
        assertEquals(40000, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("非法的目标字段"));
        assertTrue(jobPositionMapper.selectByImportFileId(importId).isEmpty());
    }

    @Test
    void skippedColumnMappingShouldNotImportThatColumn() throws Exception {
        long importId = uploadImportFile();

        // 用户指定"招聘人数"不导入(targetField 为空)
        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(mapping("招录机关", "departmentName"));
        mappings.add(mapping("岗位名称", "positionName"));
        mappings.add(mapping("职位代码", "positionCode"));
        mappings.add(mapping("招聘人数", null));
        mappings.add(mapping("薪资待遇", null));

        JsonNode data = postConfirm(importId, mappings);
        assertEquals(2, data.get("successRows").asInt());

        for (JobPosition position : jobPositionMapper.selectByImportFileId(importId)) {
            assertNull(position.getRecruitCount());
            assertNull(position.getRemark());
        }
    }

    @Test
    void jobListShouldSupportPaginationAndFilters() throws Exception {
        long importId = uploadImportFile();
        postConfirm(importId, defaultUserMappings());

        // 默认分页
        JsonNode data = getJson("/api/jobs");
        assertEquals(2, data.get("total").asLong());
        assertEquals(1, data.get("page").asInt());
        assertEquals(10, data.get("size").asInt());
        assertEquals(2, data.get("items").size());

        // 分页 size=1, 第 2 页
        data = getJson("/api/jobs?page=1&size=1");
        assertEquals(2, data.get("total").asLong());
        assertEquals(1, data.get("items").size());
        data = getJson("/api/jobs?page=2&size=1");
        assertEquals(2, data.get("total").asLong());
        assertEquals(1, data.get("items").size());
        // 超出范围的页返回空列表
        data = getJson("/api/jobs?page=3&size=1");
        assertEquals(0, data.get("items").size());

        // keyword 过滤(职位名称)
        data = getJson("/api/jobs?keyword=Java");
        assertEquals(1, data.get("total").asLong());
        assertEquals("Java开发工程师", data.get("items").get(0).get("positionName").asText());

        // 部门过滤
        data = getJson("/api/jobs?departmentName=部门B");
        assertEquals(1, data.get("total").asLong());
        assertEquals("数据分析师", data.get("items").get(0).get("positionName").asText());

        // 职位代码过滤
        data = getJson("/api/jobs?keyword=3001002");
        assertEquals(1, data.get("total").asLong());

        // 无匹配
        data = getJson("/api/jobs?keyword=不存在");
        assertEquals(0, data.get("total").asLong());

        // 列表不返回 raw_data 大字段
        assertNull(data.get("items").size() > 0 ? null : null);
        JsonNode listRow = getJson("/api/jobs?keyword=Java").get("items").get(0);
        assertTrue(listRow.get("rawData") == null || listRow.get("rawData").isNull(),
                "列表接口不应返回 rawData 大字段");
    }

    @Test
    void jobDetailShouldReturnRawDataAndRejectMissing() throws Exception {
        long importId = uploadImportFile();
        postConfirm(importId, defaultUserMappings());

        JobPosition position = jobPositionMapper.selectByImportFileId(importId).get(0);

        JsonNode data = getJson("/api/jobs/" + position.getId());
        assertEquals(position.getId(), data.get("id").asLong());
        assertEquals("Java开发工程师", data.get("positionName").asText());
        assertEquals("职位表", data.get("sourceSheet").asText());
        assertEquals(2, data.get("sourceRow").asInt());

        // 详情含 raw_data
        JsonNode raw = objectMapper.readTree(data.get("rawData").asText());
        assertEquals("部门A", raw.get("招录机关").asText());
        assertEquals("10000", raw.get("薪资待遇").asText());

        // 不存在的岗位
        MvcResult result = mockMvc.perform(get("/api/jobs/999999"))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(40404, root.get("code").asInt());
    }

    // ---------------- 工具方法 ----------------

    /** 上传测试 Excel 并返回 importId */
    private long uploadImportFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "国考岗位表.xlsx", XLSX_CONTENT_TYPE, buildImportExcel());
        MvcResult result = mockMvc.perform(multipart("/api/import/upload").file(file))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(0, root.get("code").asInt());
        return root.get("data").get("fileId").asLong();
    }

    /** 默认用户确认映射(含人工修改: 薪资待遇 -> remark) */
    private List<Map<String, String>> defaultUserMappings() {
        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(mapping("招录机关", "departmentName"));
        mappings.add(mapping("岗位名称", "positionName"));
        mappings.add(mapping("职位代码", "positionCode"));
        mappings.add(mapping("招聘人数", "recruitCount"));
        mappings.add(mapping("薪资待遇", "remark")); // 人工指定未识别列
        return mappings;
    }

    private Map<String, String> mapping(String source, String target) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("sourceField", source);
        if (target != null) {
            item.put("targetField", target);
        }
        return item;
    }

    private JsonNode postConfirm(long importId, List<Map<String, String>> mappings) throws Exception {
        JsonNode root = postConfirmRaw(importId, mappings);
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private JsonNode postConfirmRaw(long importId, List<Map<String, String>> mappings) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mappings", mappings);
        MvcResult result = mockMvc.perform(post("/api/import/" + importId + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        return readTree(result);
    }

    private JsonNode getJson(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private JsonNode readTree(MvcResult result) throws IOException {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertHeader(JsonNode node, String source, String suggested, String confidence) {
        assertEquals(source, node.get("sourceField").asText());
        if (suggested == null) {
            assertTrue(node.get("suggestedField") == null || node.get("suggestedField").isNull(),
                    source + " 应无可建议字段");
        } else {
            assertEquals(suggested, node.get("suggestedField").asText());
        }
        assertEquals(confidence, node.get("confidence").asText());
    }

    /**
     * 构造导入测试 Excel:
     * 表头: 招录机关(EXACT) / 岗位名称(ALIAS) / 职位代码(EXACT) / 招聘人数(ALIAS) / 薪资待遇(UNKNOWN)
     * 第 2 行: 正常数据
     * 第 3 行: 招聘人数="若干"(数字解析失败应容错)
     * 第 4 行: 职位代码为空(该行应失败)
     */
    private byte[] buildImportExcel() throws IOException {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("招录机关", "岗位名称", "职位代码", "招聘人数", "薪资待遇"));
        rows.add(Arrays.asList("部门A", "Java开发工程师", "3001001", "3", "10000"));
        rows.add(Arrays.asList("部门B", "数据分析师", "3001002", "若干", "8000"));
        rows.add(Arrays.asList("部门C", "网络管理员", null, "2", "7000"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out).excelType(ExcelTypeEnum.XLSX).sheet(SHEET_NAME).doWrite(rows);
        return out.toByteArray();
    }
}
