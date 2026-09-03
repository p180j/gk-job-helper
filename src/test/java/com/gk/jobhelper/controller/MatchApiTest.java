package com.gk.jobhelper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobMatch;
import com.gk.jobhelper.entity.JobMatchItem;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobMatchMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import com.gk.jobhelper.service.ExcelImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 报考资格匹配引擎 V1 接口集成测试（H2 内存库）
 * 覆盖: 单岗位匹配、结果落库、重复匹配更新、匹配详情、批量匹配、
 * MATCH/UNCERTAIN/NOT_MATCH 分页查询、异常入参。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchApiTest {

    private static final String REFERENCE_DATE = "2026-08-27";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private JobPositionMapper jobPositionMapper;

    @Autowired
    private ImportFileMapper importFileMapper;

    @Autowired
    private JobMatchMapper jobMatchMapper;

    @Autowired
    private ExcelImportService excelImportService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long profileId;
    private Long importId;
    private Long matchJobId;      // 全条件满足 -> MATCH
    private Long notMatchJobId;   // 学历不满足 -> NOT_MATCH
    private Long uncertainJobId;  // 备注存在无法自动判断的限制 -> UNCERTAIN

    @BeforeEach
    void prepareData() {
        jobMatchMapper.deleteAllItems();
        jobMatchMapper.deleteAll();
        jobPositionMapper.deleteAll();
        userProfileMapper.deleteAll();
        importFileMapper.deleteAll();

        profileId = insertProfile();
        importId = insertImportFile();
        List<Long> jobIds = insertPositions(importId);
        matchJobId = jobIds.get(0);
        notMatchJobId = jobIds.get(1);
        uncertainJobId = jobIds.get(2);
    }

    @Test
    void matchSingleShouldReturnItemsAndPersist() throws Exception {
        JsonNode data = postJson("/api/jobs/" + matchJobId + "/match",
                matchBody(REFERENCE_DATE));

        assertEquals(matchJobId.longValue(), data.get("jobId").asLong());
        assertEquals(profileId.longValue(), data.get("profileId").asLong());
        assertEquals("MATCH", data.get("result").asText());
        assertEquals(REFERENCE_DATE, data.get("referenceDate").asText());

        // 7 个条件明细，全部 MATCH（包含专业、性别和备注条件）
        JsonNode items = data.get("items");
        assertEquals(7, items.size());
        assertEquals("EDUCATION", items.get(0).get("conditionType").asText());
        assertEquals("MATCH", items.get(0).get("result").asText());
        assertEquals("本科", items.get(0).get("userValue").asText());
        assertEquals("本科及以上", items.get(0).get("requirementValue").asText());
        assertTrue(items.get(0).get("reason").asText().contains("满足"));

        assertEquals("AGE", items.get(1).get("conditionType").asText());
        // 年龄基于 birthDate + referenceDate 计算（1994-06-15 -> 32 周岁）
        assertEquals("32", items.get(1).get("userValue").asText());
        assertTrue(items.get(1).get("reason").asText().contains("2026-08-27"));

        assertEquals("POLITICAL", items.get(2).get("conditionType").asText());
        assertEquals("WORK_EXPERIENCE", items.get(3).get("conditionType").asText());
        assertEquals("MATCH", items.get(3).get("result").asText());
        assertTrue(items.get(3).get("reason").asText().contains("5年"));

        // MAJOR: 软件工程(080902) 属于 计算机类(0809)，含结构化证据
        assertEquals("MAJOR", items.get(4).get("conditionType").asText());
        assertEquals("MATCH", items.get(4).get("result").asText());
        assertEquals("软件工程(080902)", items.get(4).get("userValue").asText());
        assertEquals("计算机类", items.get(4).get("requirementValue").asText());
        assertTrue(items.get(4).get("reason").asText().contains("计算机类(0809)"));
        JsonNode evidence = items.get(4).get("evidence");
        assertNotNull(evidence);
        assertEquals("MOE_UNDERGRADUATE_2024", evidence.get("catalogCode").asText());
        assertEquals("080902", evidence.get("majorCode").asText());
        assertEquals("0809", evidence.get("parentCode").asText());

        assertEquals("GENDER", items.get(5).get("conditionType").asText());
        assertEquals("MATCH", items.get(5).get("result").asText());
        assertEquals("REMARK", items.get(6).get("conditionType").asText());
        assertEquals("MATCH", items.get(6).get("result").asText());

        // 匹配结果落库校验
        JobMatch match = jobMatchMapper.selectByProfileAndPosition(profileId, matchJobId);
        assertNotNull(match);
        assertEquals("MATCH", match.getMatchResult());
        assertEquals(LocalDate.of(2026, 8, 27), match.getReferenceDate());
        assertEquals(importId, match.getImportFileId());
        List<JobMatchItem> persistedItems = jobMatchMapper.selectItemsByMatchId(match.getId());
        assertEquals(7, persistedItems.size());
        // MAJOR 明细证据以 JSON 落库
        JobMatchItem majorItem = persistedItems.get(4);
        assertEquals("MAJOR", majorItem.getConditionType());
        assertNotNull(majorItem.getEvidence());
        assertTrue(majorItem.getEvidence().contains("MOE_UNDERGRADUATE_2024"));
    }

    @Test
    void matchSingleWithNotMatchConditionShouldAggregateNotMatch() throws Exception {
        JsonNode data = postJson("/api/jobs/" + notMatchJobId + "/match",
                matchBody(REFERENCE_DATE));
        assertEquals("NOT_MATCH", data.get("result").asText());

        JsonNode items = data.get("items");
        assertEquals("NOT_MATCH", items.get(0).get("result").asText());
        assertTrue(items.get(0).get("reason").asText().contains("低于"));
    }

    @Test
    void rematchShouldUpdateNotDuplicate() throws Exception {
        // 第一次匹配
        postJson("/api/jobs/" + matchJobId + "/match", matchBody(REFERENCE_DATE));
        // 第二次匹配（不同基准日期）
        postJson("/api/jobs/" + matchJobId + "/match", matchBody("2026-08-28"));

        // 仍只有一条 job_match（覆盖更新，不产生重复记录）
        JobMatch match = jobMatchMapper.selectByProfileAndPosition(profileId, matchJobId);
        assertNotNull(match);
        assertEquals(LocalDate.of(2026, 8, 28), match.getReferenceDate());
        assertEquals("MATCH", match.getMatchResult());
        // items 删旧插新，仍为 7 条
        assertEquals(7, jobMatchMapper.selectItemsByMatchId(match.getId()).size());
    }

    @Test
    void matchDetailShouldReturnAllItems() throws Exception {
        postJson("/api/jobs/" + matchJobId + "/match", matchBody(REFERENCE_DATE));

        JsonNode data = getJson("/api/jobs/" + matchJobId + "/match?profileId=" + profileId);
        assertEquals("MATCH", data.get("result").asText());
        assertEquals(7, data.get("items").size());
        assertEquals(REFERENCE_DATE, data.get("referenceDate").asText());
        // 详情接口反序列化返回 MAJOR 证据
        JsonNode majorItem = data.get("items").get(4);
        assertEquals("MAJOR", majorItem.get("conditionType").asText());
        assertNotNull(majorItem.get("evidence"));
        assertEquals("MOE_UNDERGRADUATE_2024",
                majorItem.get("evidence").get("catalogCode").asText());

        // UNCERTAIN 岗位明细: 各条件 reason 可读
        postJson("/api/jobs/" + uncertainJobId + "/match", matchBody(REFERENCE_DATE));
        JsonNode uncertain = getJson("/api/jobs/" + uncertainJobId + "/match?profileId=" + profileId);
        assertEquals("UNCERTAIN", uncertain.get("result").asText());
        assertEquals(7, uncertain.get("items").size());
        JsonNode ageItem = uncertain.get("items").get(1);
        assertEquals("MATCH", ageItem.get("result").asText());
        assertTrue(ageItem.get("reason").asText().contains("未设置"));
        JsonNode uncertainMajorItem = uncertain.get("items").get(4);
        assertEquals("MAJOR", uncertainMajorItem.get("conditionType").asText());
        assertEquals("MATCH", uncertainMajorItem.get("result").asText());
        assertTrue(uncertainMajorItem.get("reason").asText().contains("未设置"));
        JsonNode remarkItem = uncertain.get("items").get(6);
        assertEquals("REMARK", remarkItem.get("conditionType").asText());
        assertEquals("UNCERTAIN", remarkItem.get("result").asText());
        assertTrue(remarkItem.get("reason").asText().contains("人工核验"));
    }

    @Test
    void matchDetailWithoutMatchRecordShouldReturnNotFound() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/jobs/" + matchJobId + "/match?profileId=" + profileId))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40405, readTree(result).get("code").asInt());
    }

    @Test
    void recentImportShouldReturnCardDataWithMatchStats() throws Exception {
        executeBatch();

        JsonNode data = getJson("/api/import/recent");
        assertEquals(importId.longValue(), data.get("importId").asLong());
        assertEquals("国考岗位表.xlsx", data.get("fileName").asText());
        assertEquals("IMPORTED", data.get("status").asText());
        assertEquals(3, data.get("jobCount").asLong());
        JsonNode stats = data.get("matchStats");
        assertEquals(3, stats.get("total").asLong());
        assertEquals(1, stats.get("match").asLong());
        assertEquals(1, stats.get("uncertain").asLong());
        assertEquals(1, stats.get("notMatch").asLong());
    }

    @Test
    void recentImportWithoutProfileShouldReturnZeroStats() throws Exception {
        userProfileMapper.deleteAll();

        JsonNode data = getJson("/api/import/recent");
        // 导入记录仍返回，匹配统计全 0
        assertEquals(importId.longValue(), data.get("importId").asLong());
        JsonNode stats = data.get("matchStats");
        assertEquals(0, stats.get("total").asLong());
        assertEquals(0, stats.get("match").asLong());
    }

    @Test
    void recentImportWithoutRecordsShouldReturnNullData() throws Exception {
        importFileMapper.deleteAll();

        MvcResult result = mockMvc.perform(get("/api/import/recent"))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(0, root.get("code").asInt());
        assertTrue(root.get("data") == null || root.get("data").isNull());
    }

    @Test
    void batchExecuteShouldAggregateStatistics() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profileId", profileId);
        body.put("importId", importId);
        body.put("referenceDate", REFERENCE_DATE);

        JsonNode data = postJson("/api/match/execute", body);
        assertEquals(3, data.get("total").asLong());
        assertEquals(1, data.get("match").asLong());
        assertEquals(1, data.get("uncertain").asLong());
        assertEquals(1, data.get("notMatch").asLong());
        assertEquals(0, data.get("failedCount").asLong());
        assertEquals(0, data.get("failedItems").size());

        // 批量结果可通过分页接口查询
        JsonNode page = getJson("/api/match/result?profileId=" + profileId + "&importId=" + importId);
        assertEquals(3, page.get("total").asLong());
    }

    @Test
    void retainMostRecentImportsShouldDeleteOlderRecordAndItsMatchData() throws Exception {
        executeBatch();
        LocalDateTime base = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            insertImportFile("最新岗位表" + i + ".xlsx", base.plusSeconds(i));
        }

        excelImportService.retainMostRecentImports();

        assertEquals(5, importFileMapper.countAll());
        assertNull(importFileMapper.selectById(importId));
        assertTrue(jobPositionMapper.selectByImportFileId(importId).isEmpty());
        assertNull(jobMatchMapper.selectByProfileAndPosition(profileId, matchJobId));
    }

    @Test
    void queryMatchResultsShouldFilterByResult() throws Exception {
        executeBatch();

        // MATCH
        JsonNode match = getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId + "&result=MATCH");
        assertEquals(1, match.get("total").asLong());
        JsonNode matchRow = match.get("items").get(0);
        assertEquals("Java开发工程师", matchRow.get("positionName").asText());
        assertEquals("3001001", matchRow.get("positionCode").asText());
        assertEquals("MATCH", matchRow.get("matchResult").asText());
        assertNotNull(matchRow.get("referenceDate"));

        // UNCERTAIN
        JsonNode uncertain = getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId + "&result=UNCERTAIN");
        assertEquals(1, uncertain.get("total").asLong());
        assertEquals("网络管理员", uncertain.get("items").get(0).get("positionName").asText());

        // NOT_MATCH
        JsonNode notMatch = getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId + "&result=NOT_MATCH");
        assertEquals(1, notMatch.get("total").asLong());
        assertEquals("数据分析师", notMatch.get("items").get(0).get("positionName").asText());

        // 不按 result 过滤
        assertEquals(3, getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId).get("total").asLong());

        // 非法 result
        MvcResult invalid = mockMvc.perform(get("/api/match/result?profileId=" + profileId
                        + "&result=INVALID"))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40000, readTree(invalid).get("code").asInt());
    }

    @Test
    void queryMatchResultsShouldSupportPaging() throws Exception {
        executeBatch();

        JsonNode page1 = getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId + "&page=1&size=2");
        assertEquals(3, page1.get("total").asLong());
        assertEquals(2, page1.get("items").size());

        JsonNode page2 = getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId + "&page=2&size=2");
        assertEquals(1, page2.get("items").size());

        JsonNode page3 = getJson("/api/match/result?profileId=" + profileId
                + "&importId=" + importId + "&page=3&size=2");
        assertEquals(0, page3.get("items").size());
    }

    @Test
    void missingProfileShouldReturnProfileNotFound() throws Exception {
        Map<String, Object> body = matchBody(REFERENCE_DATE);
        body.put("profileId", 999999L);
        MvcResult result = mockMvc.perform(post("/api/jobs/" + matchJobId + "/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40401, readTree(result).get("code").asInt());
    }

    @Test
    void missingJobShouldReturnJobNotFound() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs/999999/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchBody(REFERENCE_DATE))))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40404, readTree(result).get("code").asInt());
    }

    @Test
    void missingImportShouldReturnImportNotFound() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profileId", profileId);
        body.put("importId", 999999L);
        MvcResult result = mockMvc.perform(post("/api/match/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40403, readTree(result).get("code").asInt());
    }

    @Test
    void matchWithoutProfileIdShouldFailValidation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs/" + matchJobId + "/match")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40000, readTree(result).get("code").asInt());
    }

    @Test
    void referenceDateShouldDefaultToTodayWhenMissing() throws Exception {
        JsonNode data = postJson("/api/jobs/" + matchJobId + "/match", matchBody(null));
        assertEquals(LocalDate.now().toString(), data.get("referenceDate").asText());
        // 默认基准日期下 32 岁同样满足
        assertEquals("MATCH", data.get("result").asText());
    }

    // ---------------- 数据准备 ----------------

    private Long insertProfile() {
        UserProfile profile = new UserProfile();
        profile.setName("张三");
        profile.setEducation("本科");
        profile.setPoliticalStatus("中共党员");
        profile.setWorkYears(5);
        profile.setBirthDate(LocalDate.of(1994, 6, 15));
        profile.setMajor("软件工程");
        profile.setMajorCode("080902");
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.insert(profile);
        return profile.getId();
    }

    private Long insertImportFile() {
        return insertImportFile("国考岗位表.xlsx", LocalDateTime.now());
    }

    private Long insertImportFile(String originalName, LocalDateTime createdAt) {
        ImportFile record = new ImportFile();
        record.setOriginalName(originalName);
        record.setStoredName("stored_test.xlsx");
        record.setStoredPath("./target/test-uploads/stored_test.xlsx");
        record.setFileSize(1024L);
        record.setFileType("XLSX");
        record.setSheetName("职位表");
        record.setTotalRows(3);
        record.setStatus("IMPORTED");
        record.setCreatedAt(createdAt);
        importFileMapper.insert(record);
        return record.getId();
    }

    private List<Long> insertPositions(Long importId) {
        List<JobPosition> positions = new ArrayList<>();
        // P1: 全条件满足 -> MATCH（专业要求 计算机类，MAJOR 经目录判定 MATCH）
        positions.add(buildPosition("3001001", "Java开发工程师", "本科及以上", "中共党员",
                "2年以上", "35周岁以下", "计算机类", importId));
        // P2: 学历不满足 -> NOT_MATCH（MAJOR 满足但综合聚合以 NOT_MATCH 为准）
        positions.add(buildPosition("3001002", "数据分析师", "硕士及以上", "中共党员",
                "2年以上", "35周岁以下", "计算机类", importId));
        // P3: 年龄/专业为空表示无要求；备注有无法自动判断的限制 -> 综合 UNCERTAIN
        JobPosition uncertainPosition = buildPosition("3001003", "网络管理员", "本科及以上", "中共党员",
                "2年以上", null, null, importId);
        uncertainPosition.setRemark("限本县事业单位工作5年以上人员报考");
        positions.add(uncertainPosition);
        jobPositionMapper.insertBatch(positions);
        List<Long> ids = new ArrayList<>();
        for (JobPosition position : positions) {
            ids.add(position.getId());
        }
        return ids;
    }

    private JobPosition buildPosition(String code, String name, String education,
                                      String political, String workYear, String age,
                                      String majorRequirement, Long importId) {
        JobPosition position = new JobPosition();
        position.setImportFileId(importId);
        position.setPositionCode(code);
        position.setPositionName(name);
        position.setDepartmentName("测试部门");
        position.setEducationRequirement(education);
        position.setPoliticalRequirement(political);
        position.setWorkYearRequirement(workYear);
        position.setAgeRequirement(age);
        position.setMajorRequirement(majorRequirement);
        position.setSourceSheet("职位表");
        position.setSourceRow(2);
        position.setCreatedAt(LocalDateTime.now());
        position.setUpdatedAt(LocalDateTime.now());
        return position;
    }

    // ---------------- 请求工具 ----------------

    private Map<String, Object> matchBody(String referenceDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profileId", profileId);
        if (referenceDate != null) {
            body.put("referenceDate", referenceDate);
        }
        return body;
    }

    private void executeBatch() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("profileId", profileId);
        body.put("importId", importId);
        body.put("referenceDate", REFERENCE_DATE);
        postJson("/api/match/execute", body);
    }

    private JsonNode postJson(String url, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private JsonNode getJson(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
