package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 年龄匹配规则测试。
 * 年龄一律基于 birthDate + referenceDate 计算，统一边界: "35周岁以下" => 年龄 <= 35。
 */
class AgeMatcherTest {

    private final AgeMatcher matcher = new AgeMatcher();
    /** 基准日期固定为 2026-08-27，避免测试随运行日期漂移 */
    private final LocalDate referenceDate = LocalDate.of(2026, 8, 27);
    private final MatchContext context = MatchContext.of(referenceDate);

    @Test
    void age32VsMax35ShouldMatch() {
        MatchItemResult r = match(LocalDate.of(1994, 6, 15), "35周岁以下");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertEquals("32", r.getUserValue());
        assertTrue(r.getReason().contains("2026-08-27"));
        assertTrue(r.getReason().contains("32"));
    }

    @Test
    void age36VsMax35ShouldNotMatch() {
        MatchItemResult r = match(LocalDate.of(1990, 1, 1), "35周岁以下");
        assertEquals(MatchResult.NOT_MATCH, r.getResult());
        assertEquals("36", r.getUserValue());
        assertTrue(r.getReason().contains("超过"));
    }

    @Test
    void age30VsMax30ShouldMatchOnBoundary() {
        // 统一边界: "30周岁以下" => 年龄 <= 30，恰好30周岁满足
        LocalDate birth = LocalDate.of(1996, 8, 27); // 生日当天满30
        assertEquals(MatchResult.MATCH, match(birth, "30周岁以下").getResult());
        // 生日次日出生 -> 还差一天满30 -> 29岁
        assertEquals(MatchResult.MATCH, match(birth.plusDays(1), "30周岁以下").getResult());
        // 满31周岁 -> 超过上限
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(1995, 8, 27), "30周岁以下").getResult());
        // 31岁零1天 -> 超过上限
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(1995, 8, 26), "30周岁以下").getResult());
    }

    @Test
    void rangeExpressionShouldCheckBothBounds() {
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), "18-35周岁").getResult());
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), "18至35周岁").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(1990, 1, 1), "18-35周岁").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(2009, 1, 1), "18-35周岁").getResult());
    }

    @Test
    void combinedMinAndMaxShouldWork() {
        // 组合表达 "18周岁以上、35周岁以下"
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), "18周岁以上、35周岁以下").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(1990, 1, 1), "18周岁以上、35周岁以下").getResult());
    }

    @Test
    void maxOnlyVariantsShouldParse() {
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(1990, 1, 1), "年龄不超过35周岁").getResult());
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), "年龄不超过35周岁").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(1990, 1, 1), "年龄35岁以下").getResult());
    }

    @Test
    void minOnlyShouldCheckLowerBound() {
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), "18周岁以上").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(LocalDate.of(2009, 1, 1), "18周岁以上").getResult());
    }

    @Test
    void emptyBirthDateShouldBeUncertain() {
        MatchItemResult r = match(null, "35周岁以下");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("出生日期为空"));
    }

    @Test
    void unlimitedRequirementShouldMatchWithoutBirthDate() {
        MatchItemResult r = match(null, "不限");
        assertEquals(MatchResult.MATCH, r.getResult());
    }

    @Test
    void emptyRequirementShouldBeUncertain() {
        MatchItemResult r = match(LocalDate.of(1994, 6, 15), null);
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("为空"));
    }

    @Test
    void unknownExpressionShouldBeUncertain() {
        MatchItemResult r = match(LocalDate.of(1994, 6, 15), "年龄适当");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
        assertTrue(r.getReason().contains("无法识别"));
    }

    @Test
    void birthdayExactDayCountsAsFullAge() {
        // 生日当天: 按基准日期计算恰好满32周岁
        MatchItemResult r = match(LocalDate.of(1994, 8, 27), "35周岁以下");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertEquals("32", r.getUserValue());
    }

    @Test
    void dayBeforeBirthdayShouldBeOneYearYounger() {
        // 生日未到: 1994-08-28 出生，基准 2026-08-27，年龄31而非32
        MatchItemResult r = match(LocalDate.of(1994, 8, 28), "35周岁以下");
        assertEquals(MatchResult.MATCH, r.getResult());
        assertEquals("31", r.getUserValue());
    }

    @Test
    void birthDateAfterReferenceDateShouldBeUncertain() {
        MatchItemResult r = match(LocalDate.of(2030, 1, 1), "35周岁以下");
        assertEquals(MatchResult.UNCERTAIN, r.getResult());
    }

    @Test
    void defaultReferenceDateShouldBeToday() {
        MatchContext defaultContext = MatchContext.of(null);
        MatchItemResult r = matcher.match(profile(LocalDate.of(1994, 6, 15)), position("35周岁以下"), defaultContext);
        assertEquals(LocalDate.now(), defaultContext.getReferenceDate());
        assertEquals(MatchResult.MATCH, r.getResult());
        assertTrue(r.getReason().contains(LocalDate.now().toString()));
    }

    @Test
    void fullWidthAndWhitespaceShouldNotAffectParsing() {
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), " 35 周岁以下 ").getResult());
        assertEquals(MatchResult.MATCH, match(LocalDate.of(1994, 6, 15), "18—35周岁").getResult());
    }

    private MatchItemResult match(LocalDate birthDate, String requirement) {
        return matcher.match(profile(birthDate), position(requirement), context);
    }

    private UserProfile profile(LocalDate birthDate) {
        UserProfile profile = new UserProfile();
        profile.setBirthDate(birthDate);
        return profile;
    }

    private JobPosition position(String ageRequirement) {
        JobPosition position = new JobPosition();
        position.setAgeRequirement(ageRequirement);
        return position;
    }
}
