package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.dto.CareerWorkVO;
import com.gk.jobhelper.service.JobRemarkParser;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基层工作年限匹配器。
 *
 * 规则:
 * - 空值/"不限"/"无要求"/"无"/"0年"  -> 无年限要求，MATCH
 * - "2年以上"/"2年及以上"/"满2年"/"至少2年"/"两年以上" -> workYears >= 2
 * - "具有2年以上相关工作经历"等含"相关"限定 -> UNCERTAIN（相关经历本轮无法判断）
 * - 具体岗位/行业经历（无年限数字）     -> UNCERTAIN
 * - 用户 workYears 为空且岗位有年限要求 -> UNCERTAIN
 */
@Component
public class WorkExperienceMatcher implements JobConditionMatcher {

    private static final Pattern ARABIC_NUMBER = Pattern.compile("\\d+");

    @Override
    public ConditionType support() {
        return ConditionType.WORK_EXPERIENCE;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        MatchItemResult column = matchYearColumn(profile, position);
        MatchItemResult remark = matchRemarkWork(profile, position, context);
        if (remark == null) return column;
        MatchResult overall = worse(column.getResult(), remark.getResult());
        String reason = column.getReason() + "；" + remark.getReason();
        return build(overall, remark.getUserValue(),
                remark.getRequirementValue(), reason);
    }

    private MatchItemResult matchYearColumn(UserProfile profile, JobPosition position) {
        String requirementRaw = position.getWorkYearRequirement();
        String requirement = TextNormalizer.normalize(requirementRaw);

        if (requirement.isEmpty()) {
            return build(MatchResult.MATCH, displayWorkYears(profile), requirementRaw,
                    "岗位未设置基层工作年限要求，按无基层工作年限限制处理。");
        }
        if (TextNormalizer.isUnlimited(requirement)) {
            return build(MatchResult.MATCH, displayWorkYears(profile), requirementRaw,
                    "岗位基层工作年限要求为“不限”，无年限要求。");
        }
        // 含"相关"等额外限定（相关工作经历/相关行业经验），本轮无法可靠判断
        if (requirement.contains("相关")) {
            return build(MatchResult.UNCERTAIN, displayWorkYears(profile), requirementRaw,
                    "岗位要求包含“相关工作经历”等额外限定，本轮规则无法确认是否满足。");
        }

        Integer requiredYears = extractYears(requirement);
        if (requiredYears == null) {
            return build(MatchResult.UNCERTAIN, displayWorkYears(profile), requirementRaw,
                    "无法识别岗位基层工作年限要求" + quote(requirement) + "，需人工确认。");
        }
        if (requiredYears == 0) {
            return build(MatchResult.MATCH, displayWorkYears(profile), requirementRaw,
                    "岗位基层工作年限要求为" + quote(requirement) + "，即无年限要求。");
        }

        Integer workYears = profile.getWorkYears();
        if (workYears == null) {
            return build(MatchResult.UNCERTAIN, null, requirementRaw,
                    "用户基层工作年限为空，无法判断岗位" + quote(requirement) + "的年限要求。");
        }
        if (workYears >= requiredYears) {
            return build(MatchResult.MATCH, String.valueOf(workYears), requirementRaw,
                    "岗位要求" + quote(requirement) + "，用户基层工作经历为" + workYears + "年，满足要求。");
        }
        return build(MatchResult.NOT_MATCH, String.valueOf(workYears), requirementRaw,
                "岗位要求" + quote(requirement) + "，用户基层工作经历为" + workYears + "年，不满足要求。");
    }

    private MatchItemResult matchRemarkWork(UserProfile profile, JobPosition position, MatchContext context) {
        String raw = (position.getAdditionalRequirements() == null ? "" : position.getAdditionalRequirements())
                + "；" + (position.getRemark() == null ? "" : position.getRemark());
        MatchItemResult result = null;
        for (String clause : JobRemarkParser.splitClauses(raw)) {
            if (!clause.matches("(?s).*(?:工作经历|工作经验|工作年限|行业经历|行业经验|岗位经历|岗位经验|在职人员|工作[一二两三四五六七八九十\\d]+年|从事[^，,；;。]*工作).*")) continue;
            MatchItemResult next = matchWorkClause(profile, position, context, clause);
            result = result == null ? next : build(worse(result.getResult(), next.getResult()),
                    result.getUserValue() + "；" + next.getUserValue(),
                    result.getRequirementValue() + "；" + next.getRequirementValue(),
                    result.getReason() + "；" + next.getReason());
        }
        return result;
    }

    private MatchItemResult matchWorkClause(UserProfile profile, JobPosition position, MatchContext context, String raw) {
        String text = JobRemarkParser.removeRecognizedInformationalText(TextNormalizer.normalize(raw));
        if (text.contains("非应届毕业生")) {
            String fresh = TextNormalizer.normalize(profile.getFreshGraduateStatus());
            if ("是".equals(fresh)) return build(MatchResult.MATCH, fresh, raw,
                    "该工作经历限制仅适用于非应届毕业生，档案确认应届身份。 ");
            if (!"否".equals(fresh)) return build(MatchResult.UNCERTAIN, fresh, raw,
                    "该工作经历限制仅适用于非应届毕业生，档案未确认应届身份。 ");
        }
        Matcher yearsMatcher = Pattern.compile("([一二两三四五六七八九十\\d]+)\\s*年(?:以上|及以上|工作经历|工作|经验|人员|$)").matcher(text);
        Integer requiredYears = null;
        while (yearsMatcher.find()) {
            Integer candidate = extractYears(yearsMatcher.group(1));
            if (candidate != null && (requiredYears == null || candidate > requiredYears)) requiredYears = candidate;
        }
        boolean stateOwned = text.contains("国有企事业单位") || text.contains("国有企业") || text.contains("国企");
        boolean institution = !stateOwned && text.contains("事业单位") && text.contains("工作");
        boolean county = text.contains("本县");
        boolean township = text.contains("乡镇") && text.contains("事业单位");
        boolean excludeSchoolHospital = text.contains("不含学校") || text.contains("不含学校、医院")
                || text.contains("不含学校和医院");
        boolean employed = text.contains("在职人员") || text.contains("在职职工");
        String requiredIndustry = qualifier(text, "([\\u4e00-\\u9fa5]{2,10})行业(?:工作)?(?:经历|经验)");
        String requiredPosition = qualifier(text, "([\\u4e00-\\u9fa5]{2,10})岗位(?:工作)?(?:经历|经验)");
        if (requiredPosition == null) requiredPosition = qualifier(text, "从事([\\u4e00-\\u9fa5]{2,10})工作");
        String requiredRegion = qualifier(text, "在([\\u4e00-\\u9fa5]{2,15}(?:省|市|县|区))[^，,；;。]{0,12}工作");
        if (text.matches(".*(?:相关|基层|本省|本市|本区|县级|市级|省级|或).*" )
                || text.contains("行业") && requiredIndustry == null
                || text.contains("岗位经历") && requiredPosition == null
                || text.contains("从事") && requiredPosition == null) {
            return build(MatchResult.UNCERTAIN, null, raw,
                    "工作经历还限定了行业或岗位类型，当前无法从原文可靠提取完整比较范围。 ");
        }
        if (requiredYears == null && !stateOwned && !institution && !county && !employed
                && requiredIndustry == null && requiredPosition == null && requiredRegion == null) {
            return build(MatchResult.UNCERTAIN, null, raw, "工作经历条件尚不能可靠结构化，需核实原文。 ");
        }
        if (context.getWorkExperiences().isEmpty()) {
            return build(MatchResult.UNCERTAIN, null, raw, "档案没有已确认的分段工作经历，无法核验单位、地区及年限。 ");
        }
        Set<YearMonth> qualifiedMonths = new HashSet<>();
        boolean unknown = false, possible = false;
        for (CareerWorkVO work : context.getWorkExperiences()) {
            if (work == null) continue;
            String nature = TextNormalizer.normalize(work.getOrganizationNature());
            String company = TextNormalizer.normalize(work.getCompany());
            if (requiredIndustry != null && !requiredIndustry.equals(TextNormalizer.normalize(work.getIndustry()))) {
                if (missing(work.getIndustry())) unknown = true;
                continue;
            }
            if (requiredPosition != null && !requiredPosition.equals(TextNormalizer.normalize(work.getPositionType()))) {
                if (missing(work.getPositionType())) unknown = true;
                continue;
            }
            if (requiredRegion != null && !TextNormalizer.normalize(work.getRegion()).contains(requiredRegion)) {
                if (missing(work.getRegion())) unknown = true;
                continue;
            }
            if (stateOwned && !nature.matches("国有企业|国企|中央企业|事业单位|国有事业单位|机关事业单位|国有企事业单位")) {
                if (missing(nature)) unknown = true;
                continue;
            }
            if (institution && !nature.matches("事业单位|国有事业单位|机关事业单位")) {
                if (missing(nature)) unknown = true;
                continue;
            }
            if (excludeSchoolHospital && (company.contains("学校") || company.contains("医院")
                    || TextNormalizer.normalize(work.getIndustry()).matches(".*(?:教育|医疗|医院).*"))) continue;
            if (excludeSchoolHospital && (missing(company) || missing(work.getIndustry()))) {
                unknown = true;
                continue;
            }
            if (county) {
                String district = TextNormalizer.normalize(position.getDistrict());
                String region = TextNormalizer.normalize(work.getRegion());
                if (missing(district) || missing(region)) { unknown = true; continue; }
                if (!region.contains(district)) continue;
            }
            if (township) {
                String level = TextNormalizer.normalize(work.getOrganizationLevel());
                if (missing(level)) { unknown = true; continue; }
                if (!level.matches(".*(?:乡|镇|街道|场).*")) continue;
            }
            if (employed) {
                if (work.getEmployed() == null) { unknown = true; continue; }
                if (!work.getEmployed()) continue;
            }
            if (!"USER_CONFIRMED".equals(work.getEvidenceStatus()) && !"DOCUMENT_VERIFIED".equals(work.getEvidenceStatus())) {
                unknown = true;
                continue;
            }
            possible = true;
            if (requiredYears == null) continue;
            YearMonth start = month(work.getStartDate());
            YearMonth end = month(work.getEndDate());
            if (TextNormalizer.isBlank(work.getEndDate()) && Boolean.TRUE.equals(work.getEmployed())) {
                end = YearMonth.from(context.getReferenceDate()).minusMonths(1);
            }
            if (start == null || end == null || end.isBefore(start)) { unknown = true; continue; }
            YearMonth cutoff = YearMonth.from(context.getReferenceDate()).minusMonths(1);
            if (end.isAfter(cutoff)) end = cutoff;
            if (start.isAfter(end)) continue;
            long months = ChronoUnit.MONTHS.between(start, end) + 1;
            if (months > 600) { unknown = true; continue; }
            for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) qualifiedMonths.add(cursor);
        }
        if (requiredYears == null && possible) return build(MatchResult.MATCH, "已确认工作经历", raw,
                "已确认经历满足原文中明确的单位性质及在职要求；仍请核对岗位原文。 ");
        if (requiredYears != null && qualifiedMonths.size() >= requiredYears * 12) {
            return build(MatchResult.MATCH, qualifiedMonths.size() + "个月", raw,
                    "符合单位、地区与在职限定的已确认经历累计达到" + requiredYears + "年。 ");
        }
        if (unknown) return build(MatchResult.UNCERTAIN, qualifiedMonths.size() + "个月", raw,
                "部分工作经历缺少单位性质、地区、在职、核验状态或精确起止月份，无法排除符合可能。 ");
        return build(MatchResult.NOT_MATCH, qualifiedMonths.size() + "个月", raw,
                "已确认经历不满足原文限定的单位性质、地区、在职状态"
                        + (requiredYears == null ? "。 " : "或" + requiredYears + "年要求。 "));
    }

    public MatchItemResult matchRemarkCondition(UserProfile profile, JobPosition position, MatchContext context) {
        return matchRemarkWork(profile, position, context);
    }
    public MatchItemResult matchRemarkCondition(UserProfile profile, JobPosition position, MatchContext context, String source) {
        return matchWorkClause(profile, position, context, source);
    }

    private String qualifier(String text, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(text);
        if (!matcher.find()) return null;
        return matcher.group(1).replaceFirst("^(?:具有|具备|有|从事|年以上|年及以上)", "");
    }

    private YearMonth month(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}")) return null;
        try { return YearMonth.parse(value); } catch (Exception e) { return null; }
    }

    private boolean missing(String value) {
        String text = TextNormalizer.normalize(value);
        return text.isEmpty() || text.matches("未知|不详|不清楚|待确认|待核实|未填写");
    }

    private MatchResult worse(MatchResult left, MatchResult right) {
        if (left == MatchResult.NOT_MATCH || right == MatchResult.NOT_MATCH) return MatchResult.NOT_MATCH;
        if (left == MatchResult.UNCERTAIN || right == MatchResult.UNCERTAIN) return MatchResult.UNCERTAIN;
        return MatchResult.MATCH;
    }

    /**
     * 提取要求年限: 优先阿拉伯数字，其次中文数字（两年/三年/十年）。
     * 无法提取返回 null。
     */
    private Integer extractYears(String normalizedRequirement) {
        Matcher matcher = ARABIC_NUMBER.matcher(normalizedRequirement);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return extractChineseNumber(normalizedRequirement);
    }

    /** 中文数字年限提取: 两/二/三/…/十/二十 等简单组合 */
    private Integer extractChineseNumber(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!isChineseDigit(c)) {
                continue;
            }
            int j = i;
            while (j < text.length() && isChineseDigit(text.charAt(j))) {
                j++;
            }
            Integer value = convertChineseNumber(text.substring(i, j));
            if (value != null) {
                return value;
            }
            i = j - 1;
        }
        return null;
    }

    private boolean isChineseDigit(char c) {
        return "零一二两三四五六七八九十".indexOf(c) >= 0;
    }

    /** 支持: 单字(两/三/…)、十、X十、十Y、X十Y */
    private Integer convertChineseNumber(String s) {
        if (s.length() == 1) {
            return digitValue(s.charAt(0));
        }
        if ("十".equals(s)) {
            return 10;
        }
        if (s.length() == 2 && s.charAt(1) == '十') {
            Integer high = digitValue(s.charAt(0));
            return high == null ? null : high * 10;
        }
        if (s.length() == 2 && s.charAt(0) == '十') {
            Integer low = digitValue(s.charAt(1));
            return low == null ? null : 10 + low;
        }
        if (s.length() == 3 && s.charAt(1) == '十') {
            Integer high = digitValue(s.charAt(0));
            Integer low = digitValue(s.charAt(2));
            return (high == null || low == null) ? null : high * 10 + low;
        }
        return null;
    }

    private Integer digitValue(char c) {
        switch (c) {
            case '零': return 0;
            case '一': return 1;
            case '二':
            case '两': return 2;
            case '三': return 3;
            case '四': return 4;
            case '五': return 5;
            case '六': return 6;
            case '七': return 7;
            case '八': return 8;
            case '九': return 9;
            case '十': return 10;
            default: return null;
        }
    }

    private String displayWorkYears(UserProfile profile) {
        return profile.getWorkYears() == null ? null : String.valueOf(profile.getWorkYears());
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue, String reason) {
        return new MatchItemResult(ConditionType.WORK_EXPERIENCE, result, userValue, requirementValue, reason);
    }

    private String quote(String text) {
        return "“" + text + "”";
    }
}
