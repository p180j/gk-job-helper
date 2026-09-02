package com.gk.jobhelper.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 专业目录查询接口测试（只读，不修改目录数据）:
 * GET /api/major/catalogs / GET /api/major/catalogs/{id}/items / GET /api/major/search
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MajorCatalogApiTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listCatalogsShouldReturnBuiltInCatalogs() throws Exception {
        JsonNode data = getJson("/api/major/catalogs");
        assertEquals(3, data.size());

        JsonNode undergraduate = data.get(0);
        assertEquals("MOE_UNDERGRADUATE_2024", undergraduate.get("catalogCode").asText());
        assertEquals("MOE", undergraduate.get("catalogType").asText());
        assertEquals("UNDERGRADUATE", undergraduate.get("educationLevel").asText());
        assertEquals("中华人民共和国教育部", undergraduate.get("sourceName").asText());

        JsonNode graduate = data.get(1);
        assertEquals("MOE_GRADUATE_2022", graduate.get("catalogCode").asText());
        assertEquals("GRADUATE", graduate.get("educationLevel").asText());
        assertEquals("MOE_VOCATIONAL_2021", data.get(2).get("catalogCode").asText());
    }

    @Test
    void listCatalogItemsShouldSupportKeywordFilter() throws Exception {
        JsonNode data = getJson("/api/major/catalogs/" + undergraduateCatalogId() + "/items?keyword=软件");
        assertTrue(data.get("total").asLong() >= 1);
        boolean found = false;
        for (JsonNode item : data.get("items")) {
            if ("软件工程".equals(item.get("majorName").asText())) {
                found = true;
                assertEquals("080902", item.get("majorCode").asText());
                assertEquals("计算机类", item.get("parentName").asText());
                assertEquals("MAJOR", item.get("itemLevel").asText());
            }
        }
        assertTrue(found);
    }

    @Test
    void listCatalogItemsShouldSupportMajorCodeFilter() throws Exception {
        JsonNode data = getJson("/api/major/catalogs/" + undergraduateCatalogId() + "/items?majorCode=080902");
        assertEquals(1, data.get("total").asLong());
        JsonNode item = data.get("items").get(0);
        assertEquals("软件工程", item.get("majorName").asText());
        assertEquals("计算机类", item.get("parentName").asText());
    }

    @Test
    void listCatalogItemsShouldSupportMajorNameFilterAndPaging() throws Exception {
        JsonNode page1 = getJson("/api/major/catalogs/" + undergraduateCatalogId()
                + "/items?majorName=技术&page=1&size=5");
        assertTrue(page1.get("total").asLong() > 5);
        assertEquals(5, page1.get("items").size());
        assertTrue(page1.get("total").asLong() > page1.get("items").size());
    }

    @Test
    void listCatalogItemsShouldReturnAllWithoutFilter() throws Exception {
        JsonNode data = getJson("/api/major/catalogs/" + undergraduateCatalogId() + "/items?size=100");
        assertEquals(917, data.get("total").asLong());
        assertEquals(100, data.get("items").size());
    }

    @Test
    void listCatalogItemsWithUnknownCatalogShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/major/catalogs/9999/items"))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40000, readTree(result).get("code").asInt());
    }

    @Test
    void searchShouldReturnHitsWithCatalogAndParent() throws Exception {
        JsonNode data = getJson("/api/major/search?keyword=软件工程");
        assertTrue(data.size() >= 2);

        JsonNode undergraduate = data.get(0);
        assertEquals("MOE_UNDERGRADUATE_2024", undergraduate.get("catalogCode").asText());
        assertEquals("080902", undergraduate.get("majorCode").asText());
        assertEquals("软件工程", undergraduate.get("majorName").asText());
        assertEquals("0809", undergraduate.get("parentCode").asText());
        assertEquals("计算机类", undergraduate.get("parentName").asText());

        JsonNode graduate = data.get(1);
        assertEquals("MOE_GRADUATE_2022", graduate.get("catalogCode").asText());
        assertEquals("0835", graduate.get("majorCode").asText());
        assertEquals("工学", graduate.get("parentName").asText());
    }

    @Test
    void searchShouldSupportExactCode() throws Exception {
        JsonNode data = getJson("/api/major/search?keyword=080904K");
        assertEquals(1, data.size());
        assertEquals("信息安全", data.get(0).get("majorName").asText());
        assertNotNull(data.get(0).get("parentCode"));
    }

    @Test
    void searchWithoutKeywordShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/major/search"))
                .andExpect(status().isOk()).andReturn();
        assertEquals(40000, readTree(result).get("code").asInt());
    }

    // ---------------- 请求工具 ----------------

    private JsonNode getJson(String url) throws Exception {
        MvcResult result = mockMvc.perform(get(url))
                .andExpect(status().isOk()).andReturn();
        JsonNode root = readTree(result);
        assertEquals(0, root.get("code").asInt());
        return root.get("data");
    }

    private long undergraduateCatalogId() throws Exception {
        JsonNode catalogs = getJson("/api/major/catalogs");
        for (JsonNode catalog : catalogs) {
            if ("MOE_UNDERGRADUATE_2024".equals(catalog.get("catalogCode").asText())) {
                return catalog.get("id").asLong();
            }
        }
        throw new AssertionError("未找到完整本科专业目录");
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
