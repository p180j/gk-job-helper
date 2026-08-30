package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基层工作年限匹配规则测试
 */
class WorkExperienceMatcherTest {

    private final WorkExperienceMatcher matcher = new WorkExperienceMatcher();
    private final MatchContext context = MatchContext.of(LocalDate.of(2026, 8, 27));

    @Test
    void fiveYearsVsTwoYearsAboveShouldMatch() {
        MatchItemResult r = match(5, "2年以上");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertTrue(r.getReason().contains("满足"));
        assertTrue(r.getReason().contains("5年"));
    }

    @Test
    void oneYearVsTwoYearsAboveShouldNotMatch() {
        MatchItemResult r = match(1, "2年以上");
        assertEquals(MatchResult.NOT_MATCH, r.getResult());
        assertTrue(r.getReason().contains("不满足"));
    }

    @Test
    void exactlyTwoYearsVsTwoYearsAboveShouldMatch() {
        // "2年以上"含2年（>=）
        assertEquals(MatchResult.MATCH, match(2, "2年以上").getResult());
        assertEquals(MatchResult.MATCH, match(2, "2年及以上").getResult());
        assertEquals(MatchResult.MATCH, match(2, "满2年").getResult());
        assertEquals(MatchResult.MATCH, match(2, "至少2年").getResult());
    }

    @Test
    void unlimitedShouldMatch() {
        assertEquals(MatchResult.MATCH, match(null, "不限").getResult());
        assertEquals(MatchResult.MATCH, match(0, "无要求").getResult());
        assertEquals(MatchResult.MATCH, match(1, "无").getResult());
    }

    @Test
    void zeroYearsRequirementMeansNoLimit() {
        assertEquals(MatchResult.MATCH, match(null, "0年").getResult());
        assertEquals(MatchResult.MATCH, match(0, "0年").getResult());
    }

    @Test
    void emptyUserWorkYearsShouldBeUncertain() {
        MatchItemResult r = match(null, "2年以上");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("为空"));
    }

    @Test
    void emptyRequirementShouldMeanNoRestriction() {
        MatchItemResult r = match(5, null);
        assertEquals(MatchResult.MATCH, r.getResult());
        assertTrue(r.getReason().contains("无基层工作年限限制"));
    }

    @Test
    void chineseNumberShouldBeParsed() {
        // 中文数字: 两年/三年/五年/十年
        assertEquals(MatchResult.MATCH, match(3, "两年以上").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(1, "两年以上").getResult());
        assertEquals(MatchResult.MATCH, match(3, "三年以上").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(2, "三年以上").getResult());
        assertEquals(MatchResult.MATCH, match(6, "五年以上").getResult());
        assertEquals(MatchResult.MATCH, match(11, "十年以上").getResult());
        assertEquals(MatchResult.MATCH, match(2, "两年及以上").getResult());
    }

    @Test
    void atLeastExpressionShouldParse() {
        assertEquals(MatchResult.MATCH, match(2, "至少2年").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(1, "至少2年").getResult());
    }

    @Test
    void relatedExperienceShouldBeUncertain() {
        // 含"相关"限定的经历要求无法按普通基层年限判断
        MatchItemResult r = match(5, "具有2年以上相关工作经历");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("相关"));
        assertEquals(MatchResult.UNCERTAIN, match(5, "具有相关行业2年以上经验").getResult());
        assertEquals(MatchResult.UNCERTAIN, match(5, "具有相关岗位工作经历").getResult());
    }

    @Test
    void specificExperienceWithoutYearsShouldBeUncertain() {
        assertEquals(MatchResult.UNCERTAIN, match(5, "具有网络管理工作经历").getResult());
    }

    @Test
    void unknownExpressionShouldBeUncertain() {
        MatchItemResult r = match(5, "优秀");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("无法识别"));
    }

    @Test
    void fullWidthAndWhitespaceShouldNotAffectParsing() {
        assertEquals(MatchResult.MATCH, match(3, " 2 年以上 ").getResult());
        assertEquals(MatchResult.MATCH, match(3, "两年 以上").getResult());
    }

    private MatchItemResult match(Integer workYears, String requirement) {
        UserProfile profile = new UserProfile();
        profile.setWorkYears(workYears);
        JobPosition position = new JobPosition();
        position.setWorkYearRequirement(requirement);
        return matcher.match(profile, position, context);
    }
}
