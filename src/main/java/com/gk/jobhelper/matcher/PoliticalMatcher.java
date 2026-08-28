package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 政治面貌匹配器。
 *
 * 规则:
 * - "不限"                              -> MATCH
 * - 含"优先"（党员优先/中共党员优先）    -> MATCH（优先条件不是硬性报考条件，reason 说明）
 * - "中共党员或预备党员"/"（含预备党员）" -> 允许 {党员, 预备党员}
 * - "中共党员"/"党员"（无含预备说明）    -> 要求正式党员；预备党员 -> UNCERTAIN（无法确认是否可报）
 * - "共青团员"/"群众"/"民主党派"         -> 要求对应身份
 * - 表达含糊或无法识别                  -> UNCERTAIN
 * 禁止简单 contains("党员") 直接判定，避免"不限中共党员"等语义误判。
 */
@Component
public class PoliticalMatcher implements JobConditionMatcher {

    /** 用户政治面貌身份归类 */
    private enum Identity {
        PARTY,      // 中共党员/党员
        PROBATIONARY, // 中共预备党员/预备党员
        LEAGUE,     // 共青团员/团员
        MASSES,     // 群众/无党派
        DEMOCRATIC  // 民主党派
    }

    @Override
    public ConditionType support() {
        return ConditionType.POLITICAL;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String requirementRaw = position.getPoliticalRequirement();
        String userRaw = profile.getPoliticalStatus();
        String requirement = TextNormalizer.normalize(requirementRaw);

        if (requirement.isEmpty()) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw, "岗位政治面貌要求为空，无法可靠判断。");
        }
        if (TextNormalizer.isUnlimited(requirement)) {
            return build(MatchResult.MATCH, userRaw, requirementRaw, "岗位政治面貌要求为“不限”，无政治面貌限制。");
        }
        // “优先”不是硬性报考条件，不作为排除依据
        if (requirement.contains("优先")) {
            return build(MatchResult.MATCH, userRaw, requirementRaw,
                    "岗位政治面貌要求为" + quote(requirement) + "，该条件为优先条件，不作为当前资格排除条件。");
        }
        // “不限”出现在要求中间（如“不限中共党员”）语义含糊，不猜
        if (requirement.contains("不限")) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw,
                    "岗位政治面貌要求" + quote(requirement) + "表达含糊，无法可靠判断，需人工确认。");
        }

        Identity identity = parseIdentity(userRaw);
        if (identity == null) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw,
                    "用户政治面貌" + display(userRaw) + "为空或无法识别，无法判断政治面貌要求。");
        }

        // 要求“含预备党员”（如“中共党员或预备党员”/“中共党员（含预备党员）”）
        if (requirement.contains("含预备") || (requirement.contains("党员") && requirement.contains("预备"))) {
            Set<Identity> allowed = new HashSet<>(Arrays.asList(Identity.PARTY, Identity.PROBATIONARY));
            return evaluate(allowed, identity, userRaw, requirementRaw, requirement);
        }
        if (requirement.contains("党员")) {
            // 单独“中共党员/党员”，未说明是否含预备党员
            Set<Identity> allowed = new HashSet<>(Arrays.asList(Identity.PARTY));
            return evaluateStrictParty(allowed, identity, userRaw, requirementRaw, requirement);
        }
        if (requirement.contains("团员") || requirement.contains("共青团")) {
            return evaluateSet(Identity.LEAGUE, identity, userRaw, requirementRaw, requirement);
        }
        if (requirement.contains("群众")) {
            return evaluateSet(Identity.MASSES, identity, userRaw, requirementRaw, requirement);
        }
        if (requirement.contains("民主党派")) {
            return evaluateSet(Identity.DEMOCRATIC, identity, userRaw, requirementRaw, requirement);
        }
        return build(MatchResult.UNCERTAIN, userRaw, requirementRaw,
                "无法识别岗位政治面貌要求" + quote(requirement) + "，需人工确认。");
    }

    /**
     * 用户身份归类。注意先识别“预备党员”再识别“党员”，
     * 因为“中共预备党员”包含“党员”子串。
     */
    private Identity parseIdentity(String rawPoliticalStatus) {
        String t = TextNormalizer.normalize(rawPoliticalStatus);
        if (t.isEmpty()) {
            return null;
        }
        if (t.contains("预备党员")) {
            return Identity.PROBATIONARY;
        }
        if (t.contains("党员")) {
            return Identity.PARTY;
        }
        if (t.contains("团员") || t.contains("共青团")) {
            return Identity.LEAGUE;
        }
        if (t.contains("群众") || t.contains("无党派")) {
            return Identity.MASSES;
        }
        if (t.contains("民主党派")) {
            return Identity.DEMOCRATIC;
        }
        return null;
    }

    private MatchItemResult evaluate(Set<Identity> allowed, Identity identity, String userRaw,
                                      String requirementRaw, String requirement) {
        if (allowed.contains(identity)) {
            return build(MatchResult.MATCH, userRaw, requirementRaw,
                    "用户政治面貌" + quote(userRaw) + "符合岗位" + quote(requirement) + "的要求。");
        }
        return build(MatchResult.NOT_MATCH, userRaw, requirementRaw,
                "岗位政治面貌要求为" + quote(requirement) + "，用户政治面貌为" + quote(userRaw) + "，不满足要求。");
    }

    /** 单独要求“中共党员”（未说明含预备党员）: 预备党员无法确认是否可报 -> UNCERTAIN */
    private MatchItemResult evaluateStrictParty(Set<Identity> allowed, Identity identity, String userRaw,
                                                String requirementRaw, String requirement) {
        if (allowed.contains(identity)) {
            return build(MatchResult.MATCH, userRaw, requirementRaw,
                    "用户政治面貌" + quote(userRaw) + "符合岗位" + quote(requirement) + "的要求。");
        }
        if (identity == Identity.PROBATIONARY) {
            return build(MatchResult.UNCERTAIN, userRaw, requirementRaw,
                    "岗位政治面貌要求为" + quote(requirement) + "，用户为“中共预备党员”，"
                            + "无法确认预备党员是否可报，需人工确认。");
        }
        return build(MatchResult.NOT_MATCH, userRaw, requirementRaw,
                "岗位政治面貌要求为" + quote(requirement) + "，用户政治面貌为" + quote(userRaw) + "，不满足要求。");
    }

    private MatchItemResult evaluateSet(Identity required, Identity identity, String userRaw,
                                        String requirementRaw, String requirement) {
        if (identity == required) {
            return build(MatchResult.MATCH, userRaw, requirementRaw,
                    "用户政治面貌" + quote(userRaw) + "符合岗位" + quote(requirement) + "的要求。");
        }
        return build(MatchResult.NOT_MATCH, userRaw, requirementRaw,
                "岗位政治面貌要求为" + quote(requirement) + "，用户政治面貌为" + quote(userRaw) + "，不满足要求。");
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue, String reason) {
        return new MatchItemResult(ConditionType.POLITICAL, result, userValue, requirementValue, reason);
    }

    private String quote(String raw) {
        return "“" + display(raw) + "”";
    }

    private String display(String raw) {
        return raw == null || raw.trim().isEmpty() ? "空" : raw.trim();
    }
}
