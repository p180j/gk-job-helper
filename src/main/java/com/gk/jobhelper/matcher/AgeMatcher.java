package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 年龄匹配器。
 * 年龄一律基于 birthDate + MatchContext.referenceDate 计算（禁止使用固定保存的年龄数字）。
 * 统一边界语义: "35周岁以下" => 年龄 <= 35；"18周岁以上" => 年龄 >= 18。
 * 无法解析的年龄表达 -> UNCERTAIN，不猜。
 */
@Component
public class AgeMatcher implements JobConditionMatcher {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 区间: 18-35周岁 / 18至35周岁 / 18~35岁 */
    private static final Pattern RANGE =
            Pattern.compile("(\\d+)\\s*(?:-|—|~|至|到)\\s*(\\d+)\\s*(?:周岁|岁)");
    /** 上限: 不超过35周岁 / 不高于35周岁 */
    private static final Pattern MAX_EXPLICIT =
            Pattern.compile("(?:不超过|不高于|小于)\\s*(\\d+)\\s*(?:周岁|岁)");
    /** 上限: 35周岁以下 / 35岁以下 */
    private static final Pattern MAX_SUFFIX =
            Pattern.compile("(\\d+)\\s*(?:周岁|岁)\\s*以下");
    /** 下限: 18周岁以上 / 18岁以上 */
    private static final Pattern MIN_SUFFIX =
            Pattern.compile("(\\d+)\\s*(?:周岁|岁)\\s*以上");
    /** 下限: 不低于18周岁 / 至少18周岁 */
    private static final Pattern MIN_EXPLICIT =
            Pattern.compile("(?:不低于|不小于|至少)\\s*(\\d+)\\s*(?:周岁|岁)");

    @Override
    public ConditionType support() {
        return ConditionType.AGE;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String requirementRaw = position.getAgeRequirement();
        String requirement = TextNormalizer.normalize(requirementRaw);
        LocalDate referenceDate = context.getReferenceDate();
        String dateText = referenceDate.format(DATE_FORMAT);

        if (requirement.isEmpty()) {
            return build(MatchResult.UNCERTAIN, null, requirementRaw,
                    "岗位年龄要求为空，无法可靠判断（年龄基准日期为" + dateText + "）。");
        }
        if (TextNormalizer.isUnlimited(requirement)) {
            return build(MatchResult.MATCH, null, requirementRaw,
                    "岗位年龄要求为“不限”，无年龄限制（年龄基准日期为" + dateText + "）。");
        }

        AgeRange range = parseRange(requirement);
        if (range == null) {
            return build(MatchResult.UNCERTAIN, null, requirementRaw,
                    "无法识别岗位年龄要求“" + requirement + "”，需人工确认。");
        }

        if (profile.getBirthDate() == null) {
            return build(MatchResult.UNCERTAIN, null, requirementRaw,
                    "用户出生日期为空，无法按" + dateText + "计算年龄。");
        }
        if (profile.getBirthDate().isAfter(referenceDate)) {
            return build(MatchResult.UNCERTAIN, null, requirementRaw,
                    "用户出生日期晚于基准日期" + dateText + "，数据异常，无法计算年龄。");
        }

        // 基于出生日期与基准日期的周岁（生日未到自动少一岁）
        int age = Period.between(profile.getBirthDate(), referenceDate).getYears();
        String userValue = String.valueOf(age);

        if (range.min != null && age < range.min) {
            return build(MatchResult.NOT_MATCH, userValue, requirementRaw,
                    "按" + dateText + "计算用户年龄为" + age + "周岁，低于岗位“"
                            + requirement + "”的年龄下限。");
        }
        if (range.max != null && age > range.max) {
            return build(MatchResult.NOT_MATCH, userValue, requirementRaw,
                    "按" + dateText + "计算用户年龄为" + age + "周岁，超过岗位“"
                            + requirement + "”的年龄上限。");
        }
        return build(MatchResult.MATCH, userValue, requirementRaw,
                "按" + dateText + "计算用户年龄为" + age + "周岁，满足岗位“"
                        + requirement + "”的年龄要求。");
    }

    /** 解析年龄区间，无法识别返回 null */
    private AgeRange parseRange(String normalizedRequirement) {
        Matcher matcher = RANGE.matcher(normalizedRequirement);
        if (matcher.find()) {
            return new AgeRange(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        Integer min = null;
        Integer max = null;
        matcher = MAX_EXPLICIT.matcher(normalizedRequirement);
        if (matcher.find()) {
            max = Integer.parseInt(matcher.group(1));
        }
        matcher = MAX_SUFFIX.matcher(normalizedRequirement);
        if (matcher.find()) {
            max = Integer.parseInt(matcher.group(1));
        }
        matcher = MIN_SUFFIX.matcher(normalizedRequirement);
        if (matcher.find()) {
            min = Integer.parseInt(matcher.group(1));
        }
        matcher = MIN_EXPLICIT.matcher(normalizedRequirement);
        if (matcher.find()) {
            min = Integer.parseInt(matcher.group(1));
        }
        if (min == null && max == null) {
            return null;
        }
        return new AgeRange(min, max);
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue, String reason) {
        return new MatchItemResult(ConditionType.AGE, result, userValue, requirementValue, reason);
    }

    /** 年龄区间（min/max 任一可为空表示单边限制） */
    private static class AgeRange {
        final Integer min;
        final Integer max;

        AgeRange(Integer min, Integer max) {
            this.min = min;
            this.max = max;
        }
    }
}
