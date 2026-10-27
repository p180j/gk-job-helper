package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.entity.JobPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Separates stated work sites from an agency's administrative jurisdiction. */
@Component
public class JobWorkLocationResolver {
    private static final Pattern WORK_SITE = Pattern.compile("(?:实际)?(?:工作地点|工作地区|岗位地点|工作地)\\s*(?:在|为|：|:)?\\s*([^，,；;。]+)");
    private static final Pattern MULTI_SITE = Pattern.compile("(?:多个|多处|各地|各县|各乡|若干|\\d+个县|基层站|分配|选岗)");
    private final AdministrativeDivisionCatalog divisions;
    private final ObjectMapper objectMapper;

    public JobWorkLocationResolver(AdministrativeDivisionCatalog divisions, ObjectMapper objectMapper) {
        this.divisions = divisions; this.objectMapper = objectMapper;
    }

    public Result resolve(JobPosition position) {
        String conditions = text(position.getAdditionalRequirements()) + " " + text(position.getRemark());
        if (conditions.contains("二次分配") || conditions.contains("服从分配") || conditions.contains("录用后选岗")) {
            return new Result(null, null, "MULTI_SITE", "ASSIGNMENT_RULE", conditions.trim(), "WORK_SITE");
        }
        if (position.getWorkLocation() != null && !position.getWorkLocation().trim().isEmpty()) {
            return stated(position.getWorkLocation().trim(), "SOURCE_COLUMN", "工作地点：" + position.getWorkLocation().trim());
        }
        Result explicit = fromRaw(position.getRawData());
        if (explicit != null) return explicit;
        explicit = fromText(position.getAdditionalRequirements(), "ADDITIONAL_REQUIREMENTS");
        if (explicit != null) return explicit;
        explicit = fromText(position.getRemark(), "REMARK");
        if (explicit != null) return explicit;
        explicit = fromText(position.getPositionDescription(), "POSITION_DESCRIPTION");
        if (explicit != null) return explicit;
        String agency = text(position.getOrganizationName()) + " " + text(position.getDepartmentName());
        AdministrativeDivisionCatalog.Division jurisdiction = divisions.findInText(agency);
        if (jurisdiction != null && !jurisdiction.getName().equals(jurisdiction.getProvince())) {
            return new Result(jurisdiction.getDisplayName(), jurisdiction.getCode(), "INFERRED",
                    "AGENCY_JURISDICTION", agency.trim(), "ADMIN_JURISDICTION");
        }
        return new Result(null, null, "UNKNOWN", null, null, "WORK_SITE");
    }

    private Result fromRaw(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isObject()) return null;
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String header = TextNormalizer.normalize(field.getKey()).replace(" ", "");
                if (!(header.equals("工作地点") || header.equals("实际工作地点")
                        || header.equals("工作地区") || header.equals("岗位地点"))) continue;
                String value = field.getValue().asText().trim();
                if (value.isEmpty()) continue;
                return stated(value, "SOURCE_COLUMN", field.getKey() + "：" + value);
            }
        } catch (IOException ignored) {
            // Original data remains untouched; malformed JSON supplies no reliable work-site evidence.
        }
        return null;
    }

    private Result fromText(String raw, String source) {
        Matcher matcher = WORK_SITE.matcher(TextNormalizer.normalize(raw));
        return matcher.find() ? stated(matcher.group(1), source, matcher.group()) : null;
    }

    private Result stated(String value, String source, String evidence) {
        if (MULTI_SITE.matcher(value).find()) {
            return new Result(value, null, "MULTI_SITE", source, evidence, "WORK_SITE");
        }
        AdministrativeDivisionCatalog.Division division = divisions.findInText(value);
        if (division != null) {
            return new Result(division.getDisplayName(), division.getCode(), "CONFIRMED", source, evidence, "WORK_SITE");
        }
        return new Result(value, null, "UNKNOWN", source, evidence, "WORK_SITE");
    }

    private String text(String value) { return value == null ? "" : value; }

    public static class Result {
        private final String name, code, status, source, evidence, type;
        Result(String name, String code, String status, String source, String evidence, String type) {
            this.name = name; this.code = code; this.status = status; this.source = source;
            this.evidence = evidence; this.type = type;
        }
        public String getName() { return name; }
        public String getCode() { return code; }
        public String getStatus() { return status; }
        public String getSource() { return source; }
        public String getEvidence() { return evidence; }
        public String getType() { return type; }
    }
}
