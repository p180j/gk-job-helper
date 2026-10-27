package com.gk.jobhelper.service;

import com.gk.jobhelper.common.TextNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts independently traceable conditions from the official remark. */
@Component
public class JobRemarkParser {
    private static final Pattern SERVICE = Pattern.compile("(?:须?在?(?:本机关(?:\\(单位\\))?|本单位|本岗位|艰苦边远地区)\\s*)?(?:最低服务(?:年限)?|最少服务|服务期限?不少于|服务期不少于)\\s*([一二三四五六七八九十\\d]+)\\s*年");
    private static final Pattern CERTIFICATE = Pattern.compile("(?:法律职业资格(?:证书)?A证|[\\u4e00-\\u9fa5]{0,24}(?:资格证书|资格证|执业资格证书))");
    private static final Pattern LOCATION = Pattern.compile("(?:工作地点|工作地|岗位地点|工作地区)\\s*(?:为|在|：|:)?\\s*([^，,；;。]+)");
    private static final Pattern POLITICAL = Pattern.compile("(?:政治面貌|中共党员|预备党员|共青团员|民主党派)");
    private static final Pattern IDENTITY = Pattern.compile("(?:定向招录|服务基层项目|退役士兵|退役军人|限\\d{4}届毕业生|限[^，,；;。]{1,40}报考)");
    private static final Pattern EXPERIENCE = Pattern.compile("(?:基层工作经历|相关岗位经历|工作经历|工作年限)");
    private static final Pattern ASSIGNMENT = Pattern.compile("(?:选岗|岗位分配|基层站)");
    private static final Pattern GENDER = Pattern.compile("(?:限|仅限)?(?:男性|女性)");
    private static final Pattern POLICE_PHYSICAL = Pattern.compile("(?:人民警察录用条件|体能测评|体能测试|裸眼视力|矫正视力)");

    public List<Condition> parse(String raw) {
        List<Condition> result = new ArrayList<>();
        String text = TextNormalizer.normalize(raw);
        if (text.isEmpty()) return result;
        Matcher service = SERVICE.matcher(text);
        while (service.find()) {
            Integer years = years(service.group(1));
            result.add(new Condition("MIN_SERVICE_YEARS", "POST_EMPLOYMENT", service.group(),
                    years == null ? "UNKNOWN" : "RECOGNIZED", years));
        }
        Matcher certificate = CERTIFICATE.matcher(text);
        while (certificate.find()) {
            result.add(new Condition("CERTIFICATE", "QUALIFICATION", certificate.group(), "RECOGNIZED", null));
        }
        Matcher location = LOCATION.matcher(text);
        while (location.find()) {
            result.add(new Condition("WORK_LOCATION", "INFORMATION", location.group(), "RECOGNIZED", null));
        }
        for (String fragment : text.split("[;；。]+")) {
            String clause = fragment.trim();
            if (clause.isEmpty()) continue;
            if (POLITICAL.matcher(clause).find()) {
                result.add(new Condition("POLITICAL_IDENTITY", "QUALIFICATION", clause, "NEED_CONFIRM", null));
            }
            if (GENDER.matcher(clause).find()) {
                result.add(new Condition("GENDER", "QUALIFICATION", clause, "RECOGNIZED", null));
            }
            if (POLICE_PHYSICAL.matcher(clause).find()) {
                result.add(new Condition("POLICE_PHYSICAL", "QUALIFICATION", clause, "NEED_CONFIRM", null));
            }
            if (IDENTITY.matcher(clause).find()) {
                result.add(new Condition("DIRECTED_IDENTITY", "QUALIFICATION", clause, "NEED_CONFIRM", null));
            }
            if (EXPERIENCE.matcher(clause).find()) {
                result.add(new Condition("WORK_EXPERIENCE", "QUALIFICATION", clause, "NEED_CONFIRM", null));
            }
            if (ASSIGNMENT.matcher(clause).find()) {
                result.add(new Condition("ASSIGNMENT", "POST_EMPLOYMENT", clause, "RECOGNIZED", null));
            }
            if (result.stream().noneMatch(c -> clause.contains(c.getSourceText()))) {
                String type = clause.matches(".*(?:限|须|取得|具备|报考|资格|要求).*") ? "UNKNOWN" : "OTHER_NOTE";
                result.add(new Condition(type, "UNKNOWN".equals(type) ? "QUALIFICATION" : "INFORMATION",
                        clause, "UNKNOWN".equals(type) ? "NEED_CONFIRM" : "RECOGNIZED", null));
            }
        }
        return result;
    }

    public static String removeRecognizedInformationalText(String text) {
        return LOCATION.matcher(SERVICE.matcher(text).replaceAll("")).replaceAll("");
    }

    private Integer years(String value) {
        try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { }
        String digits = "一二三四五六七八九十";
        if ("十".equals(value)) return 10;
        int index = digits.indexOf(value);
        return index >= 0 ? index + 1 : null;
    }

    public static class Condition {
        private final String type;
        private final String nature;
        private final String sourceText;
        private final String status;
        private final Integer years;

        public Condition(String type, String nature, String sourceText, String status, Integer years) {
            this.type = type; this.nature = nature; this.sourceText = sourceText;
            this.status = status; this.years = years;
        }
        public String getType() { return type; }
        public String getNature() { return nature; }
        public String getSourceText() { return sourceText; }
        public String getStatus() { return status; }
        public Integer getYears() { return years; }
        public String getScope() {
            if (!"MIN_SERVICE_YEARS".equals(type)) return null;
            if (sourceText.contains("本机关")) return "本机关（单位）";
            if (sourceText.contains("本单位")) return "本单位";
            if (sourceText.contains("艰苦边远地区")) return "艰苦边远地区";
            return "未明确";
        }
    }
}
