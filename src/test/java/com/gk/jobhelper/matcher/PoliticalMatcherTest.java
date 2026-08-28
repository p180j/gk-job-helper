package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 政治面貌匹配规则测试
 */
class PoliticalMatcherTest {

    private final PoliticalMatcher matcher = new PoliticalMatcher();
    private final MatchContext context = MatchContext.of(LocalDate.of(2026, 8, 27));

    @Test
    void partyMemberVsPartyShouldMatch() {
        assertEquals(MatchResult.MATCH, match("党员", "党员").getResult());
        assertEquals(MatchResult.MATCH, match("中共党员", "中共党员").getResult());
        assertEquals(MatchResult.MATCH, match("中共党员", "党员").getResult());
    }

    @Test
    void probationaryVsIncludeProbationaryShouldMatch() {
        // "含预备党员"/"或预备党员"允许预备党员
        assertEquals(MatchResult.MATCH, match("中共预备党员", "中共党员（含预备党员）").getResult());
        assertEquals(MatchResult.MATCH, match("中共预备党员", "党员（含预备党员）").getResult());
        assertEquals(MatchResult.MATCH, match("中共预备党员", "中共党员或预备党员").getResult());
        assertEquals(MatchResult.MATCH, match("中共党员", "中共党员或预备党员").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("群众", "中共党员或预备党员").getResult());
    }

    @Test
    void massesVsPartyShouldNotMatch() {
        MatchItemResult r = match("群众", "中共党员");
        assertEquals(MatchResult.NOT_MATCH, r.getResult());
        assertTrue(r.getReason().contains("不满足"));
        assertEquals(MatchResult.NOT_MATCH, match("共青团员", "中共党员").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("民主党派", "中共党员").getResult());
    }

    @Test
    void probationaryVsStrictPartyShouldBeUncertain() {
        // 单独"中共党员"未说明是否含预备党员，不猜
        MatchItemResult r = match("中共预备党员", "中共党员");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("预备党员"));
    }

    @Test
    void unlimitedShouldMatch() {
        MatchItemResult r = match("群众", "不限");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertEquals(MatchResult.MATCH, match(null, "不限").getResult());
    }

    @Test
    void priorityConditionShouldMatchWithoutExcluding() {
        // "党员优先"不是硬性报考条件
        MatchItemResult r = match("群众", "党员优先");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertTrue(r.getReason().contains("优先条件"));
        assertEquals(MatchResult.MATCH, match("群众", "中共党员优先").getResult());
    }

    @Test
    void leagueRequirementShouldCheckLeagueOnly() {
        assertEquals(MatchResult.MATCH, match("共青团员", "共青团员").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("群众", "共青团员").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("中共党员", "共青团员").getResult());
    }

    @Test
    void massesRequirementShouldCheckMassesOnly() {
        assertEquals(MatchResult.MATCH, match("群众", "群众").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("中共党员", "群众").getResult());
    }

    @Test
    void unknownRequirementShouldBeUncertain() {
        MatchItemResult r = match("中共党员", "海外留学人员");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("无法识别"));
    }

    @Test
    void ambiguousUnlimitedPlusRequirementShouldBeUncertain() {
        // "不限中共党员"语义含糊，不能因 contains("党员") 直接判定
        assertEquals(MatchResult.UNCERTAIN, match("中共党员", "不限中共党员").getResult());
        assertEquals(MatchResult.UNCERTAIN, match("群众", "不限中共党员").getResult());
    }

    @Test
    void emptyValuesShouldBeUncertain() {
        // 岗位要求为空
        MatchItemResult emptyRequirement = match("中共党员", null);
        assertEquals(MatchResult.UNCERTAIN, emptyRequirement.getResult());
        assertTrue(emptyRequirement.getReason().contains("为空"));
        // 用户政治面貌为空
        MatchItemResult emptyUser = match(null, "中共党员");
        assertEquals(MatchResult.UNCERTAIN, emptyUser.getResult());
        assertTrue(emptyUser.getReason().contains("无法识别"));
    }

    private MatchItemResult match(String politicalStatus, String requirement) {
        UserProfile profile = new UserProfile();
        profile.setPoliticalStatus(politicalStatus);
        JobPosition position = new JobPosition();
        position.setPoliticalRequirement(requirement);
        return matcher.match(profile, position, context);
    }
}
