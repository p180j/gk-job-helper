package com.gk.jobhelper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobMatch;
import com.gk.jobhelper.entity.JobMatchItem;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobFavoriteMapper;
import com.gk.jobhelper.mapper.JobMatchMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Iteration7ApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserProfileMapper profileMapper;
    @Autowired private ImportFileMapper importMapper;
    @Autowired private JobPositionMapper positionMapper;
    @Autowired private JobMatchMapper matchMapper;
    @Autowired private JobFavoriteMapper favoriteMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Long profileId;
    private Long importId;
    private List<Long> jobIds;

    @BeforeEach
    void prepare() {
        favoriteMapper.deleteAll();
        matchMapper.deleteAllItems();
        matchMapper.deleteAll();
        positionMapper.deleteAll();
        profileMapper.deleteAll();
        importMapper.deleteAll();

        profileId = insertProfile();
        importId = insertImport();
        jobIds = insertPositions();
        insertMatch(jobIds.get(0), "MATCH", "年龄满足岗位A要求");
        insertMatch(jobIds.get(1), "UNCERTAIN", "岗位B年龄信息需确认");
        insertMatch(jobIds.get(2), "NOT_MATCH", "岗位C年龄不符合");
        insertMatch(jobIds.get(3), "MATCH", "岗位D年龄满足");
        insertMatch(jobIds.get(4), "MATCH", "岗位E年龄满足");
        insertMatch(jobIds.get(5), "MATCH", "岗位F年龄满足");
    }

    @Test
    void filterByStatus() throws Exception {
        JsonNode data = result("status", "UNCERTAIN", "page", "1", "size", "20");
        assertEquals(1, data.get("total").asInt());
        assertEquals("UNCERTAIN", data.get("items").get(0).get("matchResult").asText());
    }

    @Test
    void filterByRegion() throws Exception {
        JsonNode data = result("region", "江西 南昌 东湖区", "page", "1", "size", "20");
        assertEquals(3, data.get("total").asInt());
        assertEquals("江西 南昌 东湖区", data.get("items").get(0).get("region").asText());
    }

    @Test
    void filterByOrganizationAndPositionKeyword() throws Exception {
        JsonNode organization = result("organizationKeyword", "数据局", "page", "1", "size", "20");
        assertEquals(1, organization.get("total").asInt());
        JsonNode position = result("positionKeyword", "软件开发", "page", "1", "size", "20");
        assertEquals(2, position.get("total").asInt());
    }

    @Test
    void filterByRecruitCount() throws Exception {
        JsonNode two = result("recruitCountMin", "2", "recruitCountMax", "2", "page", "1", "size", "20");
        assertEquals(2, two.get("total").asInt());
        JsonNode threePlus = result("recruitCountMin", "3", "page", "1", "size", "20");
        assertEquals(2, threePlus.get("total").asInt());
    }

    @Test
    void filterByCombinedConditions() throws Exception {
        JsonNode data = result("status", "MATCH", "region", "江西 南昌 东湖区",
                "educationKeyword", "本科", "majorKeyword", "计算机", "recruitCountMin", "1",
                "recruitCountMax", "1", "page", "1", "size", "20");
        assertEquals(1, data.get("total").asInt());
        assertEquals(jobIds.get(0).longValue(), data.get("items").get(0).get("jobId").asLong());
    }

    @Test
    void filterPagination() throws Exception {
        JsonNode page2 = result("status", "MATCH", "page", "2", "size", "2");
        assertEquals(4, page2.get("total").asInt());
        assertEquals(2, page2.get("items").size());
        assertEquals(2, page2.get("page").asInt());
    }

    @Test
    void favoriteShouldSucceedAndBeReturnedInResultList() throws Exception {
        favorite(jobIds.get(0));
        JsonNode data = result("status", "MATCH", "positionKeyword", "软件开发A", "page", "1", "size", "20");
        assertTrue(data.get("items").get(0).get("favorite").asBoolean());
    }

    @Test
    void duplicateFavoriteShouldBeIdempotent() throws Exception {
        favorite(jobIds.get(0));
        favorite(jobIds.get(0));
        JsonNode page = favoritePage();
        assertEquals(1, page.get("total").asInt());
    }

    @Test
    void unfavoriteShouldSucceed() throws Exception {
        favorite(jobIds.get(0));
        MvcResult response = mockMvc.perform(delete("/api/favorites/" + jobIds.get(0))
                        .param("profileId", profileId.toString()))
                .andExpect(status().isOk()).andReturn();
        assertEquals(0, root(response).get("code").asInt());
        assertFalse(favoriteMapper.exists(profileId, jobIds.get(0)));
    }

    @Test
    void favoriteListShouldBePaged() throws Exception {
        favorite(jobIds.get(0));
        favorite(jobIds.get(1));
        JsonNode page = favoritePage();
        assertEquals(2, page.get("total").asInt());
        assertEquals(2, page.get("items").size());
        assertTrue(page.get("items").get(0).get("favorite").asBoolean());
    }

    @Test
    void favoriteListShouldFilterByImportFile() throws Exception {
        favorite(jobIds.get(0));
        Long anotherImportId = insertImport();
        JobPosition anotherPosition = position("G007", "历史岗位", "南昌", "东湖区", "省信息中心", null,
                1, "本科", "计算机类");
        anotherPosition.setImportFileId(anotherImportId);
        positionMapper.insertBatch(Arrays.asList(anotherPosition));
        favorite(anotherPosition.getId());

        JsonNode page = success(get("/api/favorites").param("profileId", profileId.toString())
                .param("importId", importId.toString()).param("page", "1").param("size", "20")).get("data");
        assertEquals(1, page.get("total").asInt());
        assertEquals(jobIds.get(0).longValue(), page.get("items").get(0).get("jobId").asLong());
    }

    @Test
    void compareTwoJobs() throws Exception {
        JsonNode data = compare(jobIds.subList(0, 2));
        assertEquals(2, data.size());
        assertEquals(jobIds.get(0).longValue(), data.get(0).get("jobId").asLong());
    }

    @Test
    void compareFourJobs() throws Exception {
        JsonNode data = compare(jobIds.subList(0, 4));
        assertEquals(4, data.size());
    }

    @Test
    void compareMoreThanFourShouldBeRejected() throws Exception {
        JsonNode root = compareRoot(jobIds.subList(0, 5));
        assertEquals(40000, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("最多选择 4 个"));
    }

    @Test
    void compareMissingJobShouldBeRejected() throws Exception {
        JsonNode root = compareRoot(Arrays.asList(jobIds.get(0), 999999L));
        assertEquals(40404, root.get("code").asInt());
    }

    @Test
    void compareShouldReturnMatchReason() throws Exception {
        JsonNode data = compare(jobIds.subList(0, 2));
        assertEquals("MATCH", data.get(0).get("overallStatus").asText());
        assertEquals("年龄满足岗位A要求", data.get(0).get("matchItems").get(0).get("reason").asText());
    }

    private JsonNode result(String... filters) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = get("/api/match/result")
                .param("profileId", profileId.toString()).param("importId", importId.toString());
        for (int i = 0; i < filters.length; i += 2) request.param(filters[i], filters[i + 1]);
        return success(request).get("data");
    }

    private void favorite(Long jobId) throws Exception {
        JsonNode root = success(post("/api/favorites/" + jobId).param("profileId", profileId.toString()));
        assertEquals(0, root.get("code").asInt());
    }

    private JsonNode favoritePage() throws Exception {
        return success(get("/api/favorites").param("profileId", profileId.toString()).param("page", "1").param("size", "20")).get("data");
    }

    private JsonNode compare(List<Long> ids) throws Exception {
        JsonNode root = compareRoot(ids);
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private JsonNode compareRoot(List<Long> ids) throws Exception {
        StringBuilder csv = new StringBuilder();
        for (Long id : ids) { if (csv.length() > 0) csv.append(','); csv.append(id); }
        return success(get("/api/jobs/compare").param("profileId", profileId.toString()).param("jobIds", csv.toString()));
    }

    private JsonNode success(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return root(result);
    }

    private JsonNode root(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private Long insertProfile() {
        UserProfile profile = new UserProfile();
        profile.setName("测试档案");
        profile.setEducation("本科");
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        profileMapper.insert(profile);
        return profile.getId();
    }

    private Long insertImport() {
        ImportFile record = new ImportFile();
        record.setOriginalName("iteration7.xlsx"); record.setStoredName("iteration7.xlsx");
        record.setStoredPath("./target/iteration7.xlsx"); record.setStatus("IMPORTED");
        record.setCreatedAt(LocalDateTime.now()); importMapper.insert(record);
        return record.getId();
    }

    private List<Long> insertPositions() {
        List<JobPosition> positions = new ArrayList<>();
        positions.add(position("A001", "软件开发A", "南昌", "东湖区", "省信息中心", "技术处", 1, "本科及以上", "计算机类"));
        positions.add(position("B002", "数据分析岗", "九江", "浔阳区", "九江市数据局", "数据中心", 2, "硕士", "统计学"));
        positions.add(position("C003", "行政执法岗", "赣州", "章贡区", "赣州市城管局", null, 3, "大专及以上", "不限"));
        positions.add(position("D004", "软件开发D", "南昌", "东湖区", "省信息中心", "研发处", 4, "本科及以上", "软件工程"));
        positions.add(position("E005", "综合管理岗", "南昌", "东湖区", "省人社厅", null, 1, "本科", "管理学"));
        positions.add(position("F006", "网络运维岗", "九江", "濂溪区", "九江市工信局", null, 2, "本科", "计算机类"));
        positionMapper.insertBatch(positions);
        List<Long> ids = new ArrayList<>(); for (JobPosition p : positions) ids.add(p.getId()); return ids;
    }

    private JobPosition position(String code, String name, String city, String district, String department,
                                 String organization, int recruitCount, String education, String major) {
        JobPosition p = new JobPosition(); p.setImportFileId(importId); p.setPositionCode(code); p.setPositionName(name);
        p.setProvince("江西"); p.setCity(city); p.setDistrict(district); p.setDepartmentName(department);
        p.setOrganizationName(organization); p.setRecruitCount(recruitCount); p.setEducationRequirement(education);
        p.setMajorRequirement(major); p.setAgeRequirement("35周岁以下"); p.setSourceRow(2);
        p.setCreatedAt(LocalDateTime.now()); p.setUpdatedAt(LocalDateTime.now()); return p;
    }

    private void insertMatch(Long jobId, String status, String reason) {
        JobMatch match = new JobMatch(); match.setProfileId(profileId); match.setJobPositionId(jobId);
        match.setImportFileId(importId); match.setMatchResult(status); match.setReferenceDate(LocalDate.of(2026, 8, 29));
        match.setCreatedAt(LocalDateTime.now()); match.setUpdatedAt(LocalDateTime.now()); matchMapper.insert(match);
        JobMatchItem item = new JobMatchItem(); item.setJobMatchId(match.getId()); item.setJobPositionId(jobId);
        item.setConditionType("AGE"); item.setMatchResult(status); item.setUserValue("32");
        item.setRequirementValue("35周岁以下"); item.setReason(reason); item.setCreatedAt(LocalDateTime.now());
        matchMapper.insertItems(Arrays.asList(item));
    }
}
