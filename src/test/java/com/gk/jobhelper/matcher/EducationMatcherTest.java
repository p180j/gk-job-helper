package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学历匹配规则测试
 */
class EducationMatcherTest {

    private final EducationMatcher matcher = new EducationMatcher();
    private final MatchContext context = MatchContext.of(LocalDate.of(2026, 8, 27));

    @Test
    void bachelorVsBachelorShouldMatch() {
        // 本科 vs 本科（按仅限该层次处理）
        MatchItemResult r = match("本科", "本科");
        assertEquals(MatchResult.MATCH, r.getResult());
    }

    @Test
    void bachelorVsBachelorOrAboveShouldMatch() {
        MatchItemResult r = match("本科", "本科及以上");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertTrue(r.getReason().contains("满足"));
    }

    @Test
    void masterVsBachelorOrAboveShouldMatch() {
        assertEquals(MatchResult.MATCH, match("硕士", "本科及以上").getResult());
    }

    @Test
    void bachelorVsMasterOrAboveShouldNotMatch() {
        MatchItemResult r = match("本科", "硕士及以上");
        assertEquals(MatchResult.NOT_MATCH, r.getResult());
        assertTrue(r.getReason().contains("低于"));
    }

    @Test
    void masterVsOnlyBachelorShouldNotMatch() {
        // 必须区分: "仅限本科"与"本科及以上"不是同一规则
        MatchItemResult r = match("硕士", "仅限本科");
        assertEquals(MatchResult.NOT_MATCH, r.getResult());
        assertEquals(MatchResult.NOT_MATCH, match("硕士研究生", "仅限本科").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("博士", "仅限本科").getResult());
    }

    @Test
    void bachelorVsBachelorOrMasterShouldMatch() {
        assertEquals(MatchResult.MATCH, match("本科", "本科或硕士").getResult());
        assertEquals(MatchResult.MATCH, match("硕士", "本科或硕士").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("大专", "本科或硕士").getResult());
    }

    @Test
    void doctorVsGraduateOrAboveShouldMatch() {
        // "研究生及以上"按硕士门槛，博士满足
        assertEquals(MatchResult.MATCH, match("博士", "研究生及以上").getResult());
        assertEquals(MatchResult.MATCH, match("硕士", "研究生及以上").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("本科", "研究生及以上").getResult());
    }

    @Test
    void graduateListExpressionShouldIncludeMasterAndDoctor() {
        // "本科、研究生"允许本科/硕士/博士
        assertEquals(MatchResult.MATCH, match("本科", "本科、研究生").getResult());
        assertEquals(MatchResult.MATCH, match("硕士", "本科、研究生").getResult());
        assertEquals(MatchResult.MATCH, match("博士", "本科、研究生").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("专科", "本科、研究生").getResult());
    }

    @Test
    void juniorCollegeAndHighSchoolShouldFollowOrAboveRule() {
        assertEquals(MatchResult.MATCH, match("大专", "大专及以上").getResult());
        assertEquals(MatchResult.MATCH, match("本科", "大专及以上").getResult());
        assertEquals(MatchResult.NOT_MATCH, match("高中", "大专及以上").getResult());
        assertEquals(MatchResult.MATCH, match("高中", "高中及以上").getResult());
        // 专科层次高于高中，满足"高中及以上"
        assertEquals(MatchResult.MATCH, match("专科", "高中及以上").getResult());
    }

    @Test
    void unlimitedRequirementShouldMatch() {
        MatchItemResult r = match("本科", "不限");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertEquals(MatchResult.MATCH, match(null, "不限").getResult());
    }

    @Test
    void emptyRequirementShouldMeanNoRestriction() {
        MatchItemResult r = match("本科", null);
        assertEquals(MatchResult.MATCH, r.getResult());
        assertEquals(MatchResult.MATCH, match("本科", "  ").getResult());
        assertTrue(r.getReason().contains("无学历限制"));
    }

    @Test
    void emptyUserEducationShouldBeUncertain() {
        MatchItemResult r = match(null, "本科及以上");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("无法判断"));
    }

    @Test
    void unknownRequirementShouldBeUncertain() {
        MatchItemResult r = match("本科", "雅思7分及以上");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("无法识别"));
    }

    @Test
    void fullWidthAndWhitespaceShouldNotAffectParsing() {
        // 全角空格/换行/连续空格
        assertEquals(MatchResult.MATCH, match(" 本科 ", " 本科及以上 ").getResult());
        assertEquals(MatchResult.MATCH, match("大学本科", "本科及以上").getResult());
        assertEquals(MatchResult.MATCH, match("本科", "本科\n 及以上").getResult());
    }

    @Test
    void userValueAndRequirementValueShouldKeepRawText() {
        MatchItemResult r = match("本科", "本科及以上");
        assertEquals("本科", r.getUserValue());
        assertEquals("本科及以上", r.getRequirementValue());
    }

    private MatchItemResult match(String userEducation, String requirement) {
        UserProfile profile = new UserProfile();
        profile.setEducation(userEducation);
        JobPosition position = new JobPosition();
        position.setEducationRequirement(requirement);
        return matcher.match(profile, position, context);
    }
}
