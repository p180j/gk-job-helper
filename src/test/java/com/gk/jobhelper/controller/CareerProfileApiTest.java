package com.gk.jobhelper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.mapper.CareerProfileMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareerProfileApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserProfileMapper userProfileMapper;
    @Autowired private CareerProfileMapper careerProfileMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clean() { careerProfileMapper.deleteAll(); userProfileMapper.deleteAll(); }

    @Test
    void shouldSaveOnlyConfirmedCareerProfile() throws Exception {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", "张三"); profile.put("education", "本科"); profile.put("major", "软件工程");
        mockMvc.perform(post("/api/profile").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(profile))).andExpect(status().isOk());

        Map<String, Object> career = new LinkedHashMap<>();
        career.put("currentPosition", "Java开发工程师");
        career.put("totalWorkYears", "8年");
        career.put("careerDirections", Arrays.asList("后端开发"));
        career.put("industries", Arrays.asList("金融科技"));
        career.put("educationExperiences", Arrays.asList());
        career.put("workExperiences", Arrays.asList(new LinkedHashMap<String, Object>() {{ put("company", "示例公司"); put("position", "开发工程师"); }}));
        career.put("projectExperiences", Arrays.asList());
        career.put("skills", Arrays.asList("Java", "Spring Boot"));
        career.put("certificates", Arrays.asList("大学英语六级"));

        JsonNode saved = response(mockMvc.perform(put("/api/career-profile").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(career))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(0, saved.get("code").asInt());
        assertEquals("Java开发工程师", saved.path("data").path("currentPosition").asText());
        assertEquals("8年", saved.path("data").path("totalWorkYears").asText());
        assertEquals("Spring Boot", saved.path("data").path("skills").get(1).asText());

        JsonNode queried = response(mockMvc.perform(get("/api/career-profile")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals("开发工程师", queried.path("data").path("workExperiences").get(0).path("position").asText());
        assertEquals("金融科技", queried.path("data").path("industries").get(0).asText());
    }

    private JsonNode response(String content) throws Exception { return objectMapper.readTree(content); }
}
