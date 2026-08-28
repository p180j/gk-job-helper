package com.gk.jobhelper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 个人档案 API 集成测试（H2 内存库）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileMapper userProfileMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        userProfileMapper.deleteAll();
    }

    @Test
    void createProfileShouldPersistAndReturnId() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "张三");
        body.put("gender", "男");
        body.put("birthDate", "2000-01-15");
        body.put("politicalStatus", "中共党员");
        body.put("education", "大学本科");
        body.put("major", "法学");
        body.put("graduationDate", "2023-06-30");
        body.put("workYears", 2);
        body.put("targetRegion", "北京市");
        body.put("notes", "备考国考");

        JsonNode data = performPost(body);

        assertTrue(data.get("id").asLong() > 0);
        assertEquals("张三", data.get("name").asText());
        assertEquals("男", data.get("gender").asText());
        assertEquals("2000-01-15", data.get("birthDate").asText());
        assertEquals("中共党员", data.get("politicalStatus").asText());
        assertEquals("大学本科", data.get("education").asText());
        assertEquals("法学", data.get("major").asText());
        assertEquals("2023-06-30", data.get("graduationDate").asText());
        assertEquals(2, data.get("workYears").asInt());
        assertEquals("北京市", data.get("targetRegion").asText());
        assertEquals("备考国考", data.get("notes").asText());
    }

    @Test
    void getProfileShouldReturnCreatedProfile() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "李四");
        body.put("education", "硕士研究生");
        body.put("major", "会计学");

        performPost(body);

        MvcResult result = mockMvc.perform(get("/api/profile")).andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(0, root.get("code").asInt());
        assertEquals("李四", root.get("data").get("name").asText());
        assertEquals("硕士研究生", root.get("data").get("education").asText());
    }

    @Test
    void getProfileBeforeCreateShouldReturnNotFoundError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/profile")).andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(40401, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("尚未创建"));
    }

    @Test
    void createDuplicateProfileShouldBeRejected() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "王五");

        performPost(body);

        MvcResult result = mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(40402, root.get("code").asInt());
        assertTrue(root.get("message").asText().contains("已存在"));
    }

    @Test
    void updateProfileShouldOnlyChangeProvidedFields() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "赵六");
        body.put("education", "大学本科");
        body.put("major", "法学");
        performPost(body);

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", "赵六");
        update.put("education", "硕士研究生");
        update.put("targetRegion", "上海市");

        MvcResult result = mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(0, root.get("code").asInt());
        assertEquals("硕士研究生", root.get("data").get("education").asText());

        // 未提供的字段保持原值
        MvcResult after = mockMvc.perform(get("/api/profile")).andExpect(status().isOk()).andReturn();
        JsonNode profile = objectMapper.readTree(content(after)).get("data");
        assertEquals("法学", profile.get("major").asText());
        assertEquals("上海市", profile.get("targetRegion").asText());
        assertEquals("硕士研究生", profile.get("education").asText());
    }

    @Test
    void updateProfileBeforeCreateShouldReturnNotFoundError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "孙七");

        MvcResult result = mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(40401, root.get("code").asInt());
    }

    @Test
    void createProfileWithBlankNameShouldBeAllowed() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "");
        body.put("education", "大学本科");

        MvcResult result = mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(0, root.get("code").asInt());
    }

    @Test
    void createAndUpdateShouldPersistNewProfileFields() throws Exception {
        // Iteration 2 补充的档案字段可正常写入与查询，旧字段不受影响
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "钱九");
        body.put("education", "大学本科");
        body.put("degree", "学士");
        body.put("major", "计算机科学与技术");
        body.put("majorCode", "0809");
        body.put("freshGraduateStatus", "是");
        body.put("household", "浙江省杭州市");
        body.put("studentOrigin", "浙江省");
        body.put("serviceProjectType", "无");
        body.put("veteran", "否");
        body.put("certificates", "法律职业资格证,英语六级");

        JsonNode data = performPost(body);
        assertEquals("学士", data.get("degree").asText());
        assertEquals("0809", data.get("majorCode").asText());
        assertEquals("是", data.get("freshGraduateStatus").asText());
        assertEquals("浙江省杭州市", data.get("household").asText());
        assertEquals("浙江省", data.get("studentOrigin").asText());
        assertEquals("无", data.get("serviceProjectType").asText());
        assertEquals("否", data.get("veteran").asText());
        assertEquals("法律职业资格证,英语六级", data.get("certificates").asText());

        // 更新仅修改新增字段，其余保持原值
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", "钱九");
        update.put("degree", "硕士");
        update.put("veteran", "是");

        MvcResult result = mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(0, root.get("code").asInt());

        JsonNode profile = root.get("data");
        assertEquals("硕士", profile.get("degree").asText());
        assertEquals("是", profile.get("veteran").asText());
        assertEquals("0809", profile.get("majorCode").asText());
        assertEquals("计算机科学与技术", profile.get("major").asText());
    }

    private JsonNode performPost(Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = objectMapper.readTree(content(result));
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private String content(MvcResult result) throws java.io.UnsupportedEncodingException {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
