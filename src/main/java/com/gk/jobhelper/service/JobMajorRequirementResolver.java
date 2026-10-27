package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.constant.PositionStandardField;
import com.gk.jobhelper.entity.JobPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads level-specific official columns retained in the imported raw row. */
@Component
public class JobMajorRequirementResolver {
    private final ObjectMapper objectMapper;

    public JobMajorRequirementResolver(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public String resolve(JobPosition position) {
        if (position.getRawData() == null || position.getRawData().trim().isEmpty()) {
            return position.getMajorRequirement();
        }
        Map<String, String> levels = new LinkedHashMap<>();
        try {
            JsonNode root = objectMapper.readTree(position.getRawData());
            if (!root.isObject()) return position.getMajorRequirement();
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String header = PositionStandardField.normalize(field.getKey());
                String level = level(header);
                if (level != null && !field.getValue().isNull()) {
                    String value = field.getValue().asText().trim();
                    if (!value.isEmpty()) levels.put(level, value);
                }
            }
        } catch (IOException e) {
            return position.getMajorRequirement();
        }
        if (levels.isEmpty()) return position.getMajorRequirement();
        StringBuilder result = new StringBuilder();
        for (String level : new String[]{"研究生专业", "本科专业", "大专专业"}) {
            if (levels.containsKey(level)) {
                if (result.length() > 0) result.append('；');
                result.append(level).append('：').append(levels.get(level));
            }
        }
        return result.toString();
    }

    private String level(String header) {
        if ("研究生专业".equals(header) || "硕士专业".equals(header) || "博士专业".equals(header)) return "研究生专业";
        if ("本科专业".equals(header)) return "本科专业";
        if ("大专专业".equals(header) || "专科专业".equals(header) || "高职专业".equals(header)) return "大专专业";
        return null;
    }
}
