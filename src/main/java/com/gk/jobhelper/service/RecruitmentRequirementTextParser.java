package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.RecruitmentPosition;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Conservatively extracts only explicit, labelled items from a composite condition. */
@Component
public class RecruitmentRequirementTextParser {
    private static final Pattern ITEM = Pattern.compile("(?i)(?:^|[；;。\\n]\\s*|\\d+[、.．]\\s*)(本科专业|研究生专业|学历(?:要求)?|学位(?:要求)?|专业(?:要求)?|年龄(?:要求)?|工作(?:经验|年限)(?:要求)?)\\s*[：:]\\s*([^；;。\\n]+)");

    public void enrich(RecruitmentPosition position) {
        String raw = position.getOtherRequirement();
        if (raw == null || raw.trim().isEmpty()) return;
        Matcher matcher = ITEM.matcher(raw);
        while (matcher.find()) {
            String label = matcher.group(1);
            String value = matcher.group(2).trim();
            if (value.isEmpty()) continue;
            if (label.contains("学历") && blank(position.getEducationRequirement())) position.setEducationRequirement(value);
            else if (label.contains("学位") && blank(position.getDegreeRequirement())) position.setDegreeRequirement(value);
            else if (label.contains("专业") && blank(position.getMajorRequirement())) position.setMajorRequirement(label + "：" + value);
            else if (label.contains("年龄") && blank(position.getAgeRequirement())) position.setAgeRequirement(value);
            else if (label.contains("工作") && blank(position.getWorkYearsRequirement())) position.setWorkYearsRequirement(value);
        }
        if (blank(position.getEducationRequirement())) setFromKeyword(position, raw, "教育", "(?:^|[；;。\\n]\\s*|\\d+[、.．]\\s*)([^；;。\\n]*(?:本科|大专|硕士|博士|学历)[^；;。\\n]*)");
        if (blank(position.getMajorRequirement())) setFromKeyword(position, raw, "专业", "(?:^|[；;。\\n]\\s*|\\d+[、.．]\\s*)([^；;。\\n]*(?:专业|类)[^；;。\\n]*)");
        if (blank(position.getAgeRequirement())) setFromKeyword(position, raw, "年龄", "(?:^|[；;。\\n]\\s*|\\d+[、.．]\\s*)([^；;。\\n]*(?:年龄|周岁)[^；;。\\n]*)");
        if (blank(position.getWorkYearsRequirement())) setFromKeyword(position, raw, "工作", "(?:^|[；;。\\n]\\s*|\\d+[、.．]\\s*)([^；;。\\n]*(?:工作经验|工作年限|年以上)[^；;。\\n]*)");
    }
    private void setFromKeyword(RecruitmentPosition p, String raw, String field, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(raw);
        if (!matcher.find()) return;
        String value = matcher.group(1).trim();
        if ("教育".equals(field)) p.setEducationRequirement(value);
        else if ("专业".equals(field)) p.setMajorRequirement(value);
        else if ("年龄".equals(field)) p.setAgeRequirement(value);
        else p.setWorkYearsRequirement(value);
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
