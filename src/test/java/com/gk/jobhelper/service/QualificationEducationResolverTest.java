package com.gk.jobhelper.service;

import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserEducation;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.UserEducationMapper;
import com.gk.jobhelper.matcher.EducationMatcher;
import com.gk.jobhelper.matcher.MajorMatcher;
import com.gk.jobhelper.matcher.MatchContext;
import com.gk.jobhelper.matcher.MatchItemResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QualificationEducationResolverTest {

    @Test
    void highestEducationScopeMustNotUseAnotherEducationMajor() {
        UserEducationMapper mapper = mock(UserEducationMapper.class);
        EducationMatcher educationMatcher = mock(EducationMatcher.class);
        MajorMatcher majorMatcher = mock(MajorMatcher.class);
        QualificationEducationResolver resolver = new QualificationEducationResolver(
                mapper, educationMatcher, majorMatcher);

        UserProfile profile = new UserProfile();
        profile.setId(9L);
        UserEducation bachelor = education("本科", "计算机科学与技术", false);
        UserEducation master = education("硕士", "中国语言文学", true);
        when(mapper.selectEnabledByProfileId(9L)).thenReturn(Arrays.asList(bachelor, master));
        when(educationMatcher.match(any(), any(), any())).thenAnswer(invocation -> {
            UserProfile candidate = invocation.getArgument(0);
            return item(ConditionType.EDUCATION, MatchResult.MATCH, candidate.getEducation());
        });
        when(majorMatcher.match(any(), any(), any())).thenAnswer(invocation -> {
            UserProfile candidate = invocation.getArgument(0);
            MatchResult result = "计算机科学与技术".equals(candidate.getMajor())
                    ? MatchResult.MATCH : MatchResult.NOT_MATCH;
            return item(ConditionType.MAJOR, result, candidate.getMajor());
        });

        JobPosition position = new JobPosition();
        position.setEducationRequirement("本科及以上");
        position.setMajorRequirement("计算机科学与技术");
        position.setAdditionalRequirements("须以最高学历所学专业报考");
        List<MatchItemResult> result = resolver.resolve(profile, position, MatchContext.of(LocalDate.of(2026, 10, 27)));

        assertEquals(MatchResult.NOT_MATCH, result.get(1).getResult());
        assertTrue(result.get(1).getReason().contains("硕士 / 中国语言文学"));
    }

    private UserEducation education(String level, String major, boolean highest) {
        UserEducation education = new UserEducation();
        education.setEducationLevel(level);
        education.setMajorName(major);
        education.setHighest(highest);
        return education;
    }

    private MatchItemResult item(ConditionType type, MatchResult result, String userValue) {
        return new MatchItemResult(type, result, userValue, "要求", "判定");
    }
}
