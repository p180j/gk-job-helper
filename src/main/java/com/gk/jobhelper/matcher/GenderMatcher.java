package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.springframework.stereotype.Component;

/** 职位表结构化性别要求匹配器；备注中的性别限制仍由 RemarkMatcher 负责。 */
@Component
public class GenderMatcher implements JobConditionMatcher {

    @Override
    public ConditionType support() {
        return ConditionType.GENDER;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String requirement = position.getGenderRequirement();
        String normalizedRequirement = TextNormalizer.normalize(requirement);
        if (normalizedRequirement.isEmpty() || TextNormalizer.isUnlimited(normalizedRequirement)
                || normalizedRequirement.contains("男女不限")) {
            return build(MatchResult.MATCH, profile.getGender(), requirement, "岗位未设置性别限制。");
        }

        String requiredGender = requiredGender(normalizedRequirement);
        if (requiredGender == null) {
            return build(MatchResult.UNCERTAIN, profile.getGender(), requirement,
                    "岗位性别要求无法可靠识别，需人工确认。");
        }

        String profileGender = normalizeGender(profile.getGender());
        if (profileGender == null) {
            return build(MatchResult.UNCERTAIN, profile.getGender(), requirement,
                    "岗位要求" + display(requiredGender) + "，但档案未填写性别。");
        }
        if (requiredGender.equals(profileGender)) {
            return build(MatchResult.MATCH, profile.getGender(), requirement,
                    "档案性别为" + display(profileGender) + "，符合岗位性别要求。");
        }
        return build(MatchResult.NOT_MATCH, profile.getGender(), requirement,
                "档案性别为" + display(profileGender) + "，不符合岗位性别要求。" );
    }

    private String requiredGender(String requirement) {
        boolean male = requirement.contains("男");
        boolean female = requirement.contains("女");
        if (male == female) {
            return null;
        }
        return male ? "男" : "女";
    }

    private String normalizeGender(String gender) {
        String normalized = TextNormalizer.normalize(gender);
        if ("男".equals(normalized) || "男性".equals(normalized)) {
            return "男";
        }
        if ("女".equals(normalized) || "女性".equals(normalized)) {
            return "女";
        }
        return null;
    }

    private String display(String gender) {
        return "男".equals(gender) ? "男性" : "女".equals(gender) ? "女性" : "未填写";
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirement, String reason) {
        return new MatchItemResult(ConditionType.GENDER, result, userValue, requirement, reason);
    }
}
