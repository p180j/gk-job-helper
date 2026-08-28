package com.gk.jobhelper.service;

import com.gk.jobhelper.constant.FieldConfidence;
import com.gk.jobhelper.constant.PositionStandardField;
import com.gk.jobhelper.dto.HeaderSuggestion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字段映射建议单元测试：
 * 1. 表头与标准字段精确一致 -> EXACT
 * 2. 表头命中同义词 -> ALIAS
 * 3. 无法识别 -> UNKNOWN（不做 AI 猜测）
 */
class FieldMappingServiceTest {

    private final FieldMappingService fieldMappingService = new FieldMappingService();

    @Test
    void exactStandardHeaderShouldSuggestExact() {
        // 标准字段中文名 / 英文字段名均视为 EXACT
        assertSuggest("招录机关", "departmentName", FieldConfidence.EXACT);
        assertSuggest("招考职位", "positionName", FieldConfidence.EXACT);
        assertSuggest("职位代码", "positionCode", FieldConfidence.EXACT);
        assertSuggest("学历", "educationRequirement", FieldConfidence.EXACT);
        assertSuggest("positionName", "positionName", FieldConfidence.EXACT);
        assertSuggest("recruitCount", "recruitCount", FieldConfidence.EXACT);
    }

    @Test
    void headerWithWhitespaceAndCaseShouldStillMatchExact() {
        // 归一化：去除空白(含全角空格)并忽略大小写
        assertSuggest(" 招 录 机 关 ", "departmentName", FieldConfidence.EXACT);
        assertSuggest("PositionCode", "positionCode", FieldConfidence.EXACT);
    }

    @Test
    void synonymHeaderShouldSuggestAlias() {
        assertSuggest("招聘单位", "departmentName", FieldConfidence.ALIAS);
        assertSuggest("职位名称", "positionName", FieldConfidence.ALIAS);
        assertSuggest("岗位代码", "positionCode", FieldConfidence.ALIAS);
        assertSuggest("招聘人数", "recruitCount", FieldConfidence.ALIAS);
        assertSuggest("部门名称", "departmentName", FieldConfidence.ALIAS);
        assertSuggest("人数", "recruitCount", FieldConfidence.ALIAS);
        assertSuggest("学历要求", "educationRequirement", FieldConfidence.ALIAS);
        assertSuggest("工作地点", "city", FieldConfidence.ALIAS);
    }

    @Test
    void unrecognizedHeaderShouldReturnUnknownWithoutGuessing() {
        HeaderSuggestion suggestion = fieldMappingService.suggest("薪资待遇");
        assertEquals("薪资待遇", suggestion.getSourceField());
        assertNull(suggestion.getSuggestedField());
        assertEquals(FieldConfidence.UNKNOWN, suggestion.getConfidence());

        suggestion = fieldMappingService.suggest("笔试时间");
        assertNull(suggestion.getSuggestedField());
        assertEquals(FieldConfidence.UNKNOWN, suggestion.getConfidence());
    }

    @Test
    void blankHeaderShouldReturnUnknown() {
        HeaderSuggestion suggestion = fieldMappingService.suggest("");
        assertEquals(FieldConfidence.UNKNOWN, suggestion.getConfidence());
        assertNull(fieldMappingService.suggest(null).getSuggestedField());
    }

    @Test
    void suggestAllShouldKeepHeaderOrder() {
        List<HeaderSuggestion> suggestions = fieldMappingService.suggestAll(
                Arrays.asList("招录机关", "岗位名称", "薪资待遇"));

        assertEquals(3, suggestions.size());
        assertEquals("招录机关", suggestions.get(0).getSourceField());
        assertEquals("departmentName", suggestions.get(0).getSuggestedField());
        assertEquals(FieldConfidence.EXACT, suggestions.get(0).getConfidence());

        assertEquals("岗位名称", suggestions.get(1).getSourceField());
        assertEquals("positionName", suggestions.get(1).getSuggestedField());
        assertEquals(FieldConfidence.ALIAS, suggestions.get(1).getConfidence());

        assertEquals("薪资待遇", suggestions.get(2).getSourceField());
        assertNull(suggestions.get(2).getSuggestedField());
        assertEquals(FieldConfidence.UNKNOWN, suggestions.get(2).getConfidence());
    }

    @Test
    void isKnownTargetFieldShouldOnlyAcceptStandardNames() {
        assertTrue(fieldMappingService.isKnownTargetField("positionName"));
        assertTrue(fieldMappingService.isKnownTargetField(" departmentName "));
        assertTrue(!fieldMappingService.isKnownTargetField("salary"));
        assertTrue(!fieldMappingService.isKnownTargetField(null));
        assertTrue(!fieldMappingService.isKnownTargetField(""));
    }

    @Test
    void findColumnIndexShouldLocateHeaderIgnoringWhitespace() {
        List<String> headers = Arrays.asList("招录机关", "岗位名称", "薪资待遇");
        assertEquals(0, fieldMappingService.findColumnIndex(headers, "招录机关"));
        assertEquals(1, fieldMappingService.findColumnIndex(headers, " 岗位名称 "));
        assertEquals(-1, fieldMappingService.findColumnIndex(headers, "不存在的列"));
        assertEquals(-1, fieldMappingService.findColumnIndex(headers, null));
    }

    @Test
    void everyEnumFieldShouldBeFoundByFieldName() {
        // 枚举完整性: byFieldName 能反查所有标准字段
        for (PositionStandardField field : PositionStandardField.values()) {
            assertEquals(field, PositionStandardField.byFieldName(field.getFieldName()));
        }
        assertNull(PositionStandardField.byFieldName("noSuchField"));
    }

    private void assertSuggest(String header, String expectedField, FieldConfidence expectedConfidence) {
        HeaderSuggestion suggestion = fieldMappingService.suggest(header);
        assertEquals(header, suggestion.getSourceField());
        assertEquals(expectedField, suggestion.getSuggestedField());
        assertEquals(expectedConfidence, suggestion.getConfidence());
    }
}
