package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基层工作年限匹配器。
 *
 * 规则:
 * - "不限"/"无要求"/"无"/"0年"        -> 无年限要求，MATCH
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
        String requirementRaw = position.getWorkYearRequirement();
        String requirement = TextNormalizer.normalize(requirementRaw);

        if (requirement.isEmpty()) {
            return build(MatchResult.UNCERTAIN, displayWorkYears(profile), requirementRaw,
                    "岗位基层工作年限要求为空，无法可靠判断。");
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
