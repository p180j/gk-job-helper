package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemarkMatcherTest {

    private static final String VETERAN_OR_SERVICE_REMARK =
            "限军队服役5年（含）以上的大学生退役士兵或本省基层项目（大学生村官、三支一扶人员、西部志愿者、农村特岗教师）服务期满人员报考（含江西籍在外省基层项目服务期满的人员）";

    private final RemarkMatcher matcher = new RemarkMatcher();
    private final MatchContext context = MatchContext.of(LocalDate.of(2026, 8, 29));

    @Test
    void emptyRemarkShouldMatch() {
        assertEquals(MatchResult.MATCH, match(new UserProfile(), null).getResult());
    }

    @Test
    void nonFreshGraduateShouldNotMatchGraduateRestriction() {
        UserProfile profile = new UserProfile();
        profile.setFreshGraduateStatus("否");
        MatchItemResult result = match(profile, "限2026届普通高校毕业生报考");
        assertEquals(MatchResult.NOT_MATCH, result.getResult());
        assertTrue(result.getReason().contains("应届身份为“否”"));
    }

    @Test
    void graduationYearShouldMatchExactly() {
        UserProfile profile = new UserProfile();
        profile.setGraduationDate(LocalDate.of(2026, 6, 30));
        assertEquals(MatchResult.MATCH, match(profile, "限2026届普通高校毕业生报考").getResult());
        profile.setGraduationDate(LocalDate.of(2025, 6, 30));
        assertEquals(MatchResult.NOT_MATCH, match(profile, "限2026届普通高校毕业生报考").getResult());
    }

    @Test
    void missingGraduationYearShouldBeUncertain() {
        UserProfile profile = new UserProfile();
        profile.setFreshGraduateStatus("是");
        assertEquals(MatchResult.UNCERTAIN, match(profile, "限2026届普通高校毕业生报考").getResult());
    }

    @Test
    void genderRestrictionShouldMatchProfile() {
        UserProfile profile = new UserProfile();
        profile.setGender("女");
        assertEquals(MatchResult.MATCH, match(profile, "限女性").getResult());
        assertEquals(MatchResult.NOT_MATCH, match(profile, "限男性").getResult());
    }

    @Test
    void missingGenderShouldBeUncertain() {
        assertEquals(MatchResult.UNCERTAIN, match(new UserProfile(), "限男性").getResult());
    }

    @Test
    void combinedRestrictionsUseWorstResult() {
        UserProfile profile = new UserProfile();
        profile.setFreshGraduateStatus("否");
        profile.setGender("女");
        assertEquals(MatchResult.NOT_MATCH,
                match(profile, "限2026届普通高校毕业生报考;限女性").getResult());
    }

    @Test
    void legalCertificateRestrictionShouldUseCertificates() {
        UserProfile profile = new UserProfile();
        profile.setCertificates("法律职业资格A证");
        assertEquals(MatchResult.MATCH,
                match(profile, "已取得法律职业资格A证，或法考成绩合格").getResult());
        profile.setCertificates("教师资格证");
        assertEquals(MatchResult.NOT_MATCH,
                match(profile, "已取得法律职业资格A证").getResult());
    }

    @Test
    void cetPassShouldMeet425ThresholdAndCet6ShouldMeetCet4() {
        UserProfile profile = new UserProfile();
        profile.setEnglishLevel("CET6");
        assertEquals(MatchResult.MATCH, match(profile, "全国大学英语四级考试成绩达到425分及以上").getResult());
        assertEquals(MatchResult.MATCH, match(profile, "全国大学英语六级考试成绩达到425分及以上").getResult());
        profile.setEnglishLevel("CET4");
        assertEquals(MatchResult.NOT_MATCH, match(profile, "全国大学英语六级考试成绩达到425分及以上").getResult());
        profile.setEnglishLevel("NONE");
        assertEquals(MatchResult.NOT_MATCH, match(profile, "全国大学英语四级考试成绩达到425分及以上").getResult());
    }

    @Test
    void cetScoreAbovePassLineShouldRequireConfirmation() {
        UserProfile profile = new UserProfile();
        profile.setEnglishLevel("CET6");
        assertEquals(MatchResult.UNCERTAIN, match(profile, "大学英语六级成绩达到500分及以上").getResult());
    }

    @Test
    void unknownQualificationRestrictionShouldBeUncertain() {
        assertEquals(MatchResult.UNCERTAIN,
                match(new UserProfile(), "限本县事业单位工作5年以上人员报考").getResult());
    }

    @Test
    void informationalRemarkShouldNotBlock() {
        assertEquals(MatchResult.MATCH,
                match(new UserProfile(), "新营分局、新岗山分局各1名，根据总成绩排名选岗。").getResult());
    }

    @Test
    void nonVeteranWithoutServiceProjectShouldNotMatchAlternativeRestriction() {
        UserProfile profile = new UserProfile();
        profile.setVeteran("否");
        MatchItemResult result = match(profile, VETERAN_OR_SERVICE_REMARK);
        assertEquals(MatchResult.NOT_MATCH, result.getResult());
        assertTrue(result.getReason().contains("非退役军人"));
        assertTrue(result.getReason().contains("未填写基层项目经历"));
    }

    @Test
    void qualifiedVeteranShouldMatchAlternativeRestriction() {
        UserProfile profile = new UserProfile();
        profile.setVeteran("是");
        profile.setEducation("本科");
        profile.setNotes("大学生退役士兵，军队服役5年");
        assertEquals(MatchResult.MATCH, match(profile, VETERAN_OR_SERVICE_REMARK).getResult());
    }

    @Test
    void veteranWithoutServiceYearsShouldBeUncertain() {
        UserProfile profile = new UserProfile();
        profile.setVeteran("是");
        profile.setEducation("本科");
        MatchItemResult result = match(profile, VETERAN_OR_SERVICE_REMARK);
        assertEquals(MatchResult.UNCERTAIN, result.getResult());
        assertTrue(result.getReason().contains("未填写军队服役年限"));
    }

    @Test
    void completedAllowedServiceProjectShouldMatchAlternativeRestriction() {
        UserProfile profile = new UserProfile();
        profile.setVeteran("否");
        profile.setServiceProjectType("三支一扶（服务期满）");
        assertEquals(MatchResult.MATCH, match(profile, VETERAN_OR_SERVICE_REMARK).getResult());
    }

    @Test
    void serviceProjectWithoutCompletionShouldBeUncertain() {
        UserProfile profile = new UserProfile();
        profile.setVeteran("否");
        profile.setServiceProjectType("西部计划志愿者");
        MatchItemResult result = match(profile, VETERAN_OR_SERVICE_REMARK);
        assertEquals(MatchResult.UNCERTAIN, result.getResult());
        assertTrue(result.getReason().contains("未填写“服务期满”"));
    }

    private MatchItemResult match(UserProfile profile, String remark) {
        JobPosition position = new JobPosition();
        position.setRemark(remark);
        return matcher.match(profile, position, context);
    }
}
