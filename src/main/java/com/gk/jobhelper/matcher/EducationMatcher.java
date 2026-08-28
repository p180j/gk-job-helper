package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 学历匹配器。
 * 统一学历等级: 高中(1) < 专科(2) < 本科(3) < 硕士(4) < 博士(5)。
 *
 * 规则（必须区分三种表达，不能视为同一规则）:
 * - "本科"        -> 仅限本科（==），硕士用户 NOT_MATCH
 * - "本科及以上"  -> >= 本科
 * - "仅限本科"    -> 仅限本科（==）
 * - "本科或硕士"  -> 允许集合 {本科, 硕士}
 * - "研究生"      -> 按硕士或博士（及以上时按 >= 硕士）
 * - "不限"        -> MATCH；要求为空 -> UNCERTAIN（不武断判 MATCH）
 * - 无法识别      -> UNCERTAIN，不做猜测
 */
@Component
public class EducationMatcher implements JobConditionMatcher {

    private static final int HIGH_SCHOOL = 1;
    private static final int JUNIOR_COLLEGE = 2;
    private static final int BACHELOR = 3;
    private static final int MASTER = 4;
    private static final int DOCTOR = 5;

    @Override
    public ConditionType support() {
        return ConditionType.EDUCATION;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String requirementRaw = position.getEducationRequirement();
        String userRaw = profile.getEducation();
        String requirement = prepare(TextNormalizer.normalize(requirementRaw));
        String user = prepare(TextNormalizer.normalize(userRaw));

        // 岗位要求为空：不武断认为满足
        if (requirement.isEmpty()) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw, "岗位学历要求为空，无法可靠判断。");
        }
        if (TextNormalizer.isUnlimited(requirement)) {
            return build(MatchResult.MATCH, userRaw, requirementRaw, "岗位学历要求为“不限”，无学历限制。");
        }

        Integer userLevel = parseUserLevel(user);
        if (userLevel == null) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw,
                    "用户学历" + display(userRaw) + "为空或无法识别，无法判断学历要求。");
        }

        boolean andAbove = requirement.contains("及以上") || requirement.contains("以上");
        List<Integer> levels = extractLevels(requirement);
        boolean graduateWildcard = requirement.contains("研究生");

        if (levels.isEmpty() && !graduateWildcard) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw,
                    "无法识别岗位学历要求" + display(requirementRaw) + "，需人工确认。");
        }

        if (andAbove) {
            // "本科及以上"/"研究生及以上" -> 最低等级门槛
            int min = minLevel(levels, graduateWildcard);
            if (userLevel >= min) {
                return build(MatchResult.MATCH, userRaw, requirementRaw,
                        "用户学历" + quote(userRaw) + "满足岗位" + quote(requirementRaw) + "的学历要求。");
            }
            return build(MatchResult.NOT_MATCH, userRaw, requirementRaw,
                    "用户学历" + quote(userRaw) + "低于岗位要求" + quote(requirementRaw) + "。");
        }

        // 允许集合: "本科"(仅限) / "仅限本科" / "本科或硕士" / "研究生"(硕博)
        List<Integer> allowed = new ArrayList<>(levels);
        if (graduateWildcard) {
            allowed.add(MASTER);
            allowed.add(DOCTOR);
        }
        if (allowed.contains(userLevel)) {
            if (allowed.size() == 1) {
                return build(MatchResult.MATCH, userRaw, requirementRaw,
                        "用户学历" + quote(userRaw) + "符合岗位" + quote(requirementRaw) + "（按仅限该学历层次处理）的要求。");
            }
            return build(MatchResult.MATCH, userRaw, requirementRaw,
                    "用户学历" + quote(userRaw) + "在岗位" + quote(requirementRaw) + "允许的学历范围内。");
        }
        if (allowed.size() == 1) {
            return build(MatchResult.NOT_MATCH, userRaw, requirementRaw,
                    "岗位学历要求为" + quote(requirementRaw) + "（按仅限该学历层次处理），用户学历" + quote(userRaw) + "不符合。");
        }
        return build(MatchResult.NOT_MATCH, userRaw, requirementRaw,
                "用户学历" + quote(userRaw) + "不在岗位" + quote(requirementRaw) + "允许的学历范围内。");
    }

    /** 复合词预替换，避免"博士研究生"被当作"研究生"误判等级 */
    private String prepare(String text) {
        return text.replace("博士研究生", "博士").replace("硕士研究生", "硕士");
    }

    /** 用户学历 -> 等级，无法识别返回 null（"研究生"按硕士研究生层次处理） */
    private Integer parseUserLevel(String preparedUser) {
        if (preparedUser.isEmpty()) {
            return null;
        }
        if (preparedUser.contains("博士")) {
            return DOCTOR;
        }
        if (preparedUser.contains("硕士")) {
            return MASTER;
        }
        if (preparedUser.contains("研究生")) {
            return MASTER;
        }
        if (preparedUser.contains("本科")) {
            return BACHELOR;
        }
        if (preparedUser.contains("大专") || preparedUser.contains("专科") || preparedUser.contains("高职")) {
            return JUNIOR_COLLEGE;
        }
        if (preparedUser.contains("高中") || preparedUser.contains("中专")) {
            return HIGH_SCHOOL;
        }
        return null;
    }

    /** 岗位要求中识别出的具体等级集合 */
    private List<Integer> extractLevels(String preparedRequirement) {
        List<Integer> levels = new ArrayList<>();
        if (preparedRequirement.contains("高中") || preparedRequirement.contains("中专")) {
            levels.add(HIGH_SCHOOL);
        }
        if (preparedRequirement.contains("大专") || preparedRequirement.contains("专科")
                || preparedRequirement.contains("高职")) {
            levels.add(JUNIOR_COLLEGE);
        }
        if (preparedRequirement.contains("本科")) {
            levels.add(BACHELOR);
        }
        if (preparedRequirement.contains("硕士")) {
            levels.add(MASTER);
        }
        if (preparedRequirement.contains("博士")) {
            levels.add(DOCTOR);
        }
        return levels;
    }

    /** "及以上"规则下的最低等级（"研究生及以上"按硕士门槛） */
    private int minLevel(List<Integer> levels, boolean graduateWildcard) {
        if (levels.isEmpty() && graduateWildcard) {
            return MASTER;
        }
        int min = Integer.MAX_VALUE;
        for (Integer level : levels) {
            min = Math.min(min, level);
        }
        return min;
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue, String reason) {
        return new MatchItemResult(ConditionType.EDUCATION, result, userValue, requirementValue, reason);
    }

    private String quote(String raw) {
        return "“" + display(raw) + "”";
    }

    private String display(String raw) {
        return raw == null || raw.trim().isEmpty() ? "空" : raw.trim();
    }
}
