package com.gk.jobhelper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.ai.AiClientFactory;
import com.gk.jobhelper.ai.AiMessage;
import com.gk.jobhelper.ai.AiProviderConfig;
import com.gk.jobhelper.ai.AiRequest;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AI 仅作为 Excel 职位页识别增强；异常或未配置时由调用方回退到规则识别。 */
@Component
public class ExcelSheetAiAssistant {
    private final AiClientFactory aiClientFactory;
    private final ObjectMapper objectMapper;

    public ExcelSheetAiAssistant(AiClientFactory aiClientFactory, ObjectMapper objectMapper) {
        this.aiClientFactory = aiClientFactory;
        this.objectMapper = objectMapper;
    }

    public List<String> suggestPositionSheets(AiProviderConfig config, List<ExcelPreviewResult> sheets) {
        if (!configured(config) || sheets == null || sheets.isEmpty()) return Collections.emptyList();
        try {
            List<Map<String, Object>> metadata = new ArrayList<>();
            for (ExcelPreviewResult sheet : sheets) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("sheetName", sheet.getSheetName());
                item.put("headers", sheet.getHeaders());
                item.put("rowCount", sheet.getTotalRows());
                metadata.add(item);
            }
            AiRequest request = new AiRequest();
            request.setMessages(java.util.Arrays.asList(
                    new AiMessage("system", "You classify Excel worksheets. Return JSON only: {\"positionSheetNames\":[\"exact sheet name\"]}. Select only sheets containing job-position rows, never instruction, summary, or directory sheets."),
                    new AiMessage("user", objectMapper.writeValueAsString(metadata))));
            Map<String, List<String>> result = objectMapper.readValue(
                    aiClientFactory.get(config.getProvider()).chat(config, request).getContent(),
                    new TypeReference<Map<String, List<String>>>() { });
            return result.getOrDefault("positionSheetNames", Collections.<String>emptyList());
        } catch (java.io.IOException | RuntimeException ignored) {
            // AI 仅增强体验，失败必须保持原有本地规则可用，也不记录配置或 Key。
            return Collections.emptyList();
        }
    }

    private boolean configured(AiProviderConfig config) {
        return config != null && notBlank(config.getProvider()) && notBlank(config.getModel())
                && notBlank(config.getBaseUrl()) && notBlank(config.getApiKey());
    }

    private boolean notBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
