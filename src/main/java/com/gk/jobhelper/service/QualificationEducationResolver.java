package com.gk.jobhelper.service;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserEducation;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.UserEducationMapper;
import com.gk.jobhelper.matcher.EducationMatcher;
import com.gk.jobhelper.matcher.MajorMatcher;
import com.gk.jobhelper.matcher.MatchContext;
import com.gk.jobhelper.matcher.MatchItemResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 以一条教育经历为候选单位同时评估学历和专业，禁止跨经历拼接 MATCH。
 */
@Service
public class QualificationEducationResolver {
    private final UserEducationMapper userEducationMapper;
    private final EducationMatcher educationMatcher;
    private final MajorMatcher majorMatcher;

    public QualificationEducationResolver(UserEducationMapper userEducationMapper, EducationMatcher educationMatcher,
                                          MajorMatcher majorMatcher) {
        this.userEducationMapper = userEducationMapper;
        this.educationMatcher = educationMatcher;
        this.majorMatcher = majorMatcher;
    }

    public List<MatchItemResult> resolve(UserProfile profile, JobPosition position, MatchContext context) {
        List<UserEducation> candidates = userEducationMapper.selectEnabledByProfileId(profile.getId());
        if (candidates == null || candidates.isEmpty()) {
            return evaluate(profile, position, context, null);
        }
        Candidate best = null;
        for (UserEducation education : candidates) {
            List<MatchItemResult> items = evaluate(profile, position, context, education);
            Candidate candidate = new Candidate(education, items);
            if (best == null || candidate.rank() > best.rank()) {
                best = candidate;
            }
        }
        return appendCandidateReason(best.items, best.education);
    }

    private List<MatchItemResult> evaluate(UserProfile profile, JobPosition position, MatchContext context,
                                           UserEducation education) {
        UserProfile candidate = education == null ? profile : profileForEducation(profile, education);
        return Arrays.asList(educationMatcher.match(candidate, position, context),
                majorMatcher.match(candidate, position, context));
    }

    private UserProfile profileForEducation(UserProfile source, UserEducation education) {
        UserProfile profile = new UserProfile();
        profile.setId(source.getId());
        profile.setEducation(education.getEducationLevel());
        profile.setDegree(education.getDegree());
        profile.setMajor(education.getMajorName());
        profile.setMajorCode(education.getMajorCode());
        profile.setMajorEducationLevel(education.getEducationLevel());
        return profile;
    }

    private List<MatchItemResult> appendCandidateReason(List<MatchItemResult> items, UserEducation education) {
        if (education == null) {
            return items;
        }
        String suffix = "（采用教育经历：" + education.getEducationLevel() + " / "
                + (education.getMajorName() == null ? "专业未填" : education.getMajorName()) + "）";
        List<MatchItemResult> result = new ArrayList<>();
        for (MatchItemResult item : items) {
            result.add(new MatchItemResult(item.getConditionType(), item.getResult(), item.getUserValue(),
                    item.getRequirementValue(), item.getReason() + suffix, item.getEvidence()));
        }
        return result;
    }

    private static class Candidate {
        private final UserEducation education;
        private final List<MatchItemResult> items;
        private Candidate(UserEducation education, List<MatchItemResult> items) { this.education = education; this.items = items; }
        private int rank() {
            boolean uncertain = false;
            for (MatchItemResult item : items) {
                if (item.getResult() == MatchResult.NOT_MATCH) return 0;
                if (item.getResult() == MatchResult.UNCERTAIN) uncertain = true;
            }
            return uncertain ? 1 : 2;
        }
    }
}
