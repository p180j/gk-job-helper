package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.dto.CareerWorkVO;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkExperienceQualificationTest {
    private final WorkExperienceMatcher matcher = new WorkExperienceMatcher();

    @Test
    void conditionalStateOwnedEmploymentNeedsAllEvidence() {
        JobPosition position = new JobPosition();
        position.setAdditionalRequirements("非应届毕业生应为具有2年以上工作经历的国有企事业单位在职人员");
        UserProfile profile = new UserProfile();
        profile.setFreshGraduateStatus("否");
        CareerWorkVO work = experience("国有企业", "2022-01", "2024-12");
        assertEquals(MatchResult.MATCH, match(profile, position, work));
        assertEquals(MatchResult.MATCH, new RemarkMatcher().match(profile, position,
                MatchContext.of(LocalDate.of(2026, 10, 27), Collections.singletonList(work))).getResult());
        position.setRemark("在本单位最低服务年限为5年");
        assertEquals(MatchResult.MATCH, match(profile, position, work));
        work.setEvidenceStatus("未核验");
        assertEquals(MatchResult.UNCERTAIN, match(profile, position, work));
        work.setEvidenceStatus("USER_CONFIRMED");
        work.setOrganizationNature("非国有企业");
        assertEquals(MatchResult.NOT_MATCH, match(profile, position, work));
        work.setOrganizationNature("民营企业");
        assertEquals(MatchResult.NOT_MATCH, match(profile, position, work));
        work.setOrganizationNature(null);
        assertEquals(MatchResult.UNCERTAIN, match(profile, position, work));
        profile.setFreshGraduateStatus("是");
        assertEquals(MatchResult.MATCH, match(profile, position, work));
    }

    @Test
    void countyTownshipRequirementRejectsSchoolAndKeepsMissingDatesUncertain() {
        JobPosition position = new JobPosition();
        position.setDistrict("安义县");
        position.setAdditionalRequirements("限本县乡镇（场、街道）事业单位（不含学校、医院）工作5年以上人员报考");
        UserProfile profile = new UserProfile();
        CareerWorkVO work = experience("事业单位", "2018-01", "2024-12");
        work.setRegion("江西省南昌市安义县");
        work.setOrganizationLevel("乡镇");
        work.setIndustry("农业");
        assertEquals(MatchResult.MATCH, match(profile, position, work));
        assertEquals(MatchResult.MATCH, new RemarkMatcher().match(profile, position,
                MatchContext.of(LocalDate.of(2026, 10, 27), Collections.singletonList(work))).getResult());
        work.setCompany("安义县某学校");
        assertEquals(MatchResult.NOT_MATCH, match(profile, position, work));
        work.setCompany("安义县某乡镇单位");
        work.setStartDate("2018");
        assertEquals(MatchResult.UNCERTAIN, match(profile, position, work));
    }

    private MatchResult match(UserProfile profile, JobPosition position, CareerWorkVO work) {
        MatchContext context = MatchContext.of(LocalDate.of(2026, 10, 27), Collections.singletonList(work));
        return matcher.match(profile, position, context).getResult();
    }

    @Test
    void industryYearsOnlyAccumulateMatchingNonOverlappingMonths() {
        JobPosition position = new JobPosition();
        position.setRemark("具有2年以上金融行业工作经历");
        CareerWorkVO first = experience("国有企业", "2020-01", "2021-06");
        first.setIndustry("金融");
        CareerWorkVO second = experience("国有企业", "2021-01", "2021-12");
        second.setIndustry("金融");
        MatchContext context = MatchContext.of(LocalDate.of(2026, 10, 27), Arrays.asList(first, second));
        assertEquals(MatchResult.MATCH, matcher.match(new UserProfile(), position, context).getResult());
        second.setIndustry("农业");
        assertEquals(MatchResult.NOT_MATCH, matcher.match(new UserProfile(), position, context).getResult());
        second.setIndustry(null);
        assertEquals(MatchResult.UNCERTAIN, matcher.match(new UserProfile(), position, context).getResult());
    }

    private CareerWorkVO experience(String nature, String start, String end) {
        CareerWorkVO work = new CareerWorkVO();
        work.setCompany("某单位");
        work.setOrganizationNature(nature);
        work.setStartDate(start);
        work.setEndDate(end);
        work.setEmployed(true);
        work.setEvidenceStatus("USER_CONFIRMED");
        return work;
    }
}
