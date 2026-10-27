package com.gk.jobhelper.service;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.entity.JobPosition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts independently traceable conditions from the official remark. */
@Component
public class JobRemarkParser {
    public static final String PARSER_VERSION = "GENERAL_EXAM_V2";
    private static final Pattern SERVICE = Pattern.compile("(?:须\\s*)?(?:在?(?:本机关(?:\\(单位\\))?|本单位|本岗位|本职位|艰苦边远地区)\\s*)?(?:最低服务(?:年限)?|最少服务|服务期限?不少于|服务期不少于)\\s*(?:为|不少于|至少)?\\s*([一二三四五六七八九十\\d]+)\\s*年");
    private static final Pattern CERTIFICATE = Pattern.compile("(?:法律职业资格(?:证书)?\\s*\\(?A证\\)?|[\\u4e00-\\u9fa5]{0,24}(?:资格证书|资格证|执业资格证书))");
    private static final Pattern LOCATION = Pattern.compile("(?:工作地点|工作地|岗位地点|工作地区)\\s*(?:为|在|：|:)?\\s*([^，,；;。]+)");
    private static final Pattern POLITICAL = Pattern.compile("(?:政治面貌|中共党员|预备党员|共青团员|民主党派)");
    private static final Pattern IDENTITY = Pattern.compile("(?:定向招录|服务基层项目|退役士兵|退役军人|限\\d{4}届毕业生|限[^，,；;。]{1,40}报考)");
    private static final Pattern EXPERIENCE = Pattern.compile("(?:工作经历|工作经验|工作年限|行业经历|行业经验|岗位经历|岗位经验|工作[一二两三四五六七八九十\\d]+年|在职人员|从事[^，,；;。]*工作)");
    private static final Pattern ASSIGNMENT = Pattern.compile("(?:选岗|岗位分配|基层站)");
    private static final Pattern GENDER = Pattern.compile("(?:限|仅限)?(?:男性|女性)");
    private static final Pattern POLICE_PHYSICAL = Pattern.compile("(?:人民警察录用条件|体能测评|体能测试|裸眼视力|矫正视力)");
    private static final Pattern GRADUATE = Pattern.compile("(?:限)?(?:\\d{4}届|应届|非应届)(?:普通高校|高校|普通高等学校)?毕业生");
    private static final Pattern MAJOR_SCOPE = Pattern.compile("(?:最高学历对应专业|最低学历及以上各学历阶段对应专业之一|本科或研究生所学专业|仅限主修专业|第二学位|辅修专业)");
    private static final Pattern EDUCATION_SCOPE = Pattern.compile("(?:各阶段均需(?:同时)?取得相应学历(?:和|学)?学位|与填报学历及专业相应的学位)");
    private static final Pattern DISQUALIFICATION = Pattern.compile("[^，,；;。]{0,100}不得报考");
    private static final Pattern LANGUAGE = Pattern.compile("(?:大学英语[四六46]级|雅思|托福|英语专业[四八]级)");
    private static final Pattern INFORMATION = Pattern.compile("(?:本单位)?不(?:提供|安排)(?:宿舍|住宿)|(?:咨询|联系)(?:电话|方式)[:：]?[^，,；;。]+|(?:报名|资格复审)(?:时)?(?:需|须|应)?(?:提交|提供)[^，,；;。]+|(?:面试|专业能力测试)(?:比例|时间|地点|安排|方式)[^，,；;。]*");

    public List<Condition> parse(JobPosition position) {
        List<Condition> result = new ArrayList<>();
        result.addAll(parse(position.getAdditionalRequirements(), "additionalRequirements"));
        result.addAll(parse(position.getRemark(), "remark"));
        return result;
    }

    public List<Condition> parse(String raw) {
        return parse(raw, "remark");
    }

    public List<Condition> parse(String raw, String sourceField) {
        List<Condition> result = new ArrayList<>();
        String text = TextNormalizer.normalize(raw);
        if (text.isEmpty()) return result;
        Matcher service = SERVICE.matcher(text);
        while (service.find()) {
            Integer years = years(service.group(1));
            result.add(new Condition("MIN_SERVICE_YEARS", "POST_EMPLOYMENT", service.group(),
                    years == null ? "UNKNOWN" : "RECOGNIZED", years, sourceField));
        }
        Matcher location = LOCATION.matcher(text);
        while (location.find()) {
            result.add(new Condition("WORK_LOCATION", "INFORMATION", location.group(), "RECOGNIZED", null, sourceField));
        }
        for (String fragment : splitClauses(raw)) {
            String clause = fragment.trim();
            if (clause.isEmpty()) continue;
            Matcher certificate = CERTIFICATE.matcher(clause);
            while (certificate.find()) {
                result.add(new Condition("CERTIFICATE", "QUALIFICATION", clause, "RECOGNIZED", null, sourceField, certificate.group()));
            }
            if (LANGUAGE.matcher(clause).find()) {
                result.add(new Condition("LANGUAGE_EXAM", "QUALIFICATION", clause, "RECOGNIZED", null, sourceField));
            }
            if (POLITICAL.matcher(clause).find()) {
                result.add(new Condition("POLITICAL_IDENTITY", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (GENDER.matcher(clause).find()) {
                result.add(new Condition("GENDER", "QUALIFICATION", clause, "RECOGNIZED", null, sourceField));
            }
            if (POLICE_PHYSICAL.matcher(clause).find()) {
                result.add(new Condition("POLICE_PHYSICAL", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (GRADUATE.matcher(clause).find()) {
                result.add(new Condition("GRADUATE_STATUS", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (MAJOR_SCOPE.matcher(clause).find()) {
                result.add(new Condition("MAJOR_SCOPE", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (EDUCATION_SCOPE.matcher(clause).find()) {
                result.add(new Condition("EDUCATION_SCOPE", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (DISQUALIFICATION.matcher(clause).find()) {
                result.add(new Condition("DISQUALIFICATION", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            boolean explicitDirectedIdentity = clause.matches(
                    ".*(?:定向招录|服务基层项目|退役士兵|退役军人|限\\d{4}届毕业生).*");
            boolean anotherTypedRestriction = CERTIFICATE.matcher(clause).find() || POLITICAL.matcher(clause).find()
                    || GENDER.matcher(clause).find() || POLICE_PHYSICAL.matcher(clause).find()
                    || GRADUATE.matcher(clause).find() || EXPERIENCE.matcher(clause).find();
            if (IDENTITY.matcher(clause).find() && (explicitDirectedIdentity || !anotherTypedRestriction)) {
                result.add(new Condition("DIRECTED_IDENTITY", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (EXPERIENCE.matcher(clause).find()) {
                result.add(new Condition("WORK_EXPERIENCE", "QUALIFICATION", clause, "NEED_CONFIRM", null, sourceField));
            }
            if (ASSIGNMENT.matcher(clause).find()) {
                result.add(new Condition("ASSIGNMENT", "POST_EMPLOYMENT", clause, "RECOGNIZED", null, sourceField));
            }
            if (result.stream().noneMatch(c -> clause.contains(c.getSourceText()))) {
                String remainder = removeRecognizedInformationalText(clause);
                String type = remainder.matches(".*(?:限|须|需|取得|具备|具有|报考|资格|要求|应为|不得).*") ? "UNKNOWN" : "OTHER_NOTE";
                result.add(new Condition(type, "UNKNOWN".equals(type) ? "QUALIFICATION" : "INFORMATION",
                        clause, "UNKNOWN".equals(type) ? "NEED_CONFIRM" : "RECOGNIZED", null, sourceField));
            }
        }
        return result;
    }

    public static String removeRecognizedInformationalText(String text) {
        return INFORMATION.matcher(LOCATION.matcher(SERVICE.matcher(text).replaceAll("")).replaceAll("")).replaceAll("");
    }

    /** Keep separators inside parentheses within the same logical clause. */
    public static List<String> splitClauses(String raw) {
        List<String> clauses = new ArrayList<>();
        if (raw == null) return clauses;
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < raw.length(); index++) {
            char value = raw.charAt(index);
            if (value == '(' || value == '（' || value == '[' || value == '【') depth++;
            if (value == ')' || value == '）' || value == ']' || value == '】') depth = Math.max(0, depth - 1);
            if (depth == 0 && (value == ';' || value == '；' || value == '。' || value == '\n' || value == '\r')) {
                addClause(clauses, current);
            } else current.append(value);
        }
        addClause(clauses, current);
        return clauses;
    }

    private static void addClause(List<String> clauses, StringBuilder text) {
        String clause = TextNormalizer.normalize(text.toString()).replaceFirst("^\\d+[.、]\\s*", "");
        if (!clause.isEmpty()) clauses.add(clause);
        text.setLength(0);
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
        private final String sourceField;
        private final String matchedText;

        public Condition(String type, String nature, String sourceText, String status, Integer years) {
            this(type, nature, sourceText, status, years, "remark");
        }
        public Condition(String type, String nature, String sourceText, String status, Integer years, String sourceField) {
            this(type, nature, sourceText, status, years, sourceField, sourceText);
        }
        public Condition(String type, String nature, String sourceText, String status, Integer years, String sourceField, String matchedText) {
            this.type = type; this.nature = nature; this.sourceText = sourceText;
            this.status = status; this.years = years; this.sourceField = sourceField; this.matchedText = matchedText;
        }
        public String getType() { return type; }
        public String getNature() { return nature; }
        public String getSourceText() { return sourceText; }
        public String getStatus() { return status; }
        public Integer getYears() { return years; }
        public String getSourceField() { return sourceField; }
        public String getMatchedText() { return matchedText; }
        public String getParserVersion() { return PARSER_VERSION; }
        public String getRuleId() { return "REMARK_" + type; }
        public String getOperator() {
            if ("MIN_SERVICE_YEARS".equals(type)) return "GTE";
            if ("DISQUALIFICATION".equals(type)) return "NOT";
            if (sourceText.contains("非应届毕业生应为") || sourceText.matches(".*专业(?:须|需|应|必须)(?:取得|具有|持有).*")) return "IF_THEN";
            return sourceText.contains("或") || sourceText.contains("之一") || sourceText.contains("亦可") ? "OR" : "AND";
        }
        public Object getExpectedValue() { return years == null ? matchedText : years; }
        public String getApplicability() {
            if (sourceText.contains("非应届毕业生")) return "NON_FRESH_GRADUATE";
            if (sourceText.contains("应届毕业生")) return "FRESH_GRADUATE";
            return "ALL";
        }
        public String getConfidence() { return "RECOGNIZED".equals(status) ? "HIGH" : "LOW"; }
        public String getScope() {
            if (!"MIN_SERVICE_YEARS".equals(type)) return null;
            if (sourceText.contains("本机关")) return "本机关（单位）";
            if (sourceText.contains("本单位")) return "本单位";
            if (sourceText.contains("艰苦边远地区")) return "艰苦边远地区";
            return "未明确";
        }
    }
}
