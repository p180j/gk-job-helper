package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.RecruitmentPosition;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Shared labelled-text parser for document paragraphs and extracted PDF text. */
@Component
public class RecruitmentPositionTextExtractor {
    private static final Pattern FIELD = Pattern.compile("(?m)^\\s*(岗位名称|职位名称|招聘岗位|招聘单位|单位|部门|科室|招聘人数|计划人数|人数|工作地点|地点|学历要求|学历|学位要求|学位|本科专业|研究生专业|专业要求|专业|年龄要求|年龄|工作经验|工作年限|岗位职责|工作职责|职位描述|任职要求|资格条件|岗位条件|招聘条件|其他要求)\\s*[：:]\\s*(.+?)\\s*$");
    private final RecruitmentExcelHeaderNormalizer normalizer;
    private final RecruitmentPositionFieldMapper mapper;
    public RecruitmentPositionTextExtractor(RecruitmentExcelHeaderNormalizer normalizer, RecruitmentPositionFieldMapper mapper) { this.normalizer = normalizer; this.mapper = mapper; }
    public List<RecruitmentPosition> extract(String text, Long noticeId, Long attachmentId, String sourceType, String sourcePrefix) {
        List<RecruitmentPosition> out = new ArrayList<>();
        if (text == null) return out;
        Matcher matcher = FIELD.matcher(text.replace('\r', '\n').replaceAll("\\n{2,}", "\\n"));
        Map<String, String> data = new LinkedHashMap<>(); int block = 1, line = 1; StringBuilder raw = new StringBuilder();
        while (matcher.find()) {
            String label = matcher.group(1); String field = normalizer.normalize(label);
            if (field == null) continue;
            if ("POSITION_NAME".equals(field) && data.containsKey(field)) { add(out, data, noticeId, attachmentId, sourceType, sourcePrefix, block++, line, raw.toString()); data = new LinkedHashMap<>(); raw = new StringBuilder(); }
            String value = matcher.group(2).trim();
            if (("本科专业".equals(label) || "研究生专业".equals(label)) && data.containsKey("MAJOR")) data.put("MAJOR", data.get("MAJOR") + "；" + label + "：" + value);
            else if ("本科专业".equals(label) || "研究生专业".equals(label)) data.put("MAJOR", label + "：" + value);
            else data.put(field, value);
            raw.append(matcher.group().trim()).append('\n');
            line += countLines(matcher.group());
        }
        add(out, data, noticeId, attachmentId, sourceType, sourcePrefix, block, line, raw.toString());
        return out;
    }
    private void add(List<RecruitmentPosition> out, Map<String, String> data, Long noticeId, Long attachmentId, String sourceType, String prefix, int block, int line, String raw) {
        if (data.size() < 3 || mapper.skip(data.get("POSITION_NAME"), data)) return;
        out.add(mapper.position(data, noticeId, attachmentId, sourceType, prefix + "[" + block + "]", line, raw.trim()));
    }
    private int countLines(String value) { int count = 0; for (int i = 0; i < value.length(); i++) if (value.charAt(i) == '\n') count++; return count; }
}
