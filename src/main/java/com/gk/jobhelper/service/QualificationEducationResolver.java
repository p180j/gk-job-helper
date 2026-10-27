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
import org.springframework.dao.DataAccessException;

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
    /** 旧数据库尚未执行教育经历迁移时，自动退回到原档案字段。 */
    private volatile boolean educationTableAvailable = true;

    public QualificationEducationResolver(UserEducationMapper userEducationMapper, EducationMatcher educationMatcher,
                                          MajorMatcher majorMatcher) {
        this.userEducationMapper = userEducationMapper;
        this.educationMatcher = educationMatcher;
        this.majorMatcher = majorMatcher;
    }

    public List<MatchItemResult> resolve(UserProfile profile, JobPosition position, MatchContext context) {
        String scopeText = (position.getAdditionalRequirements() == null ? "" : position.getAdditionalRequirements())
                + "；" + (position.getRemark() == null ? "" : position.getRemark());
        boolean highestMajor = scopeText.contains("最高学历对应专业") || scopeText.contains("最高学历所学专业")
                || scopeText.contains("最高学历所学主修专业");
        boolean unsupportedScope = scopeText.contains("各阶段均需") || scopeText.contains("仅限主修专业")
                || scopeText.contains("第二学位") || scopeText.contains("辅修专业");
        if (!educationTableAvailable) {
            return scopeResult(evaluate(profile, position, context, null), highestMajor || unsupportedScope);
        }
        List<UserEducation> candidates;
        try {
            candidates = userEducationMapper.selectEnabledByProfileId(profile.getId());
        } catch (DataAccessException e) {
            educationTableAvailable = false;
            return scopeResult(evaluate(profile, position, context, null), highestMajor || unsupportedScope);
        }
        if (candidates == null || candidates.isEmpty()) {
            return scopeResult(evaluate(profile, position, context, null), highestMajor || unsupportedScope);
        }
        if (highestMajor) {
            List<UserEducation> highest = new ArrayList<>();
            for (UserEducation education : candidates) if (Boolean.TRUE.equals(education.getHighest())) highest.add(education);
            if (highest.size() != 1) {
                return scopeResult(evaluate(profile, position, context, null), true);
            }
            candidates = highest;
        }
        Candidate best = null;
        for (UserEducation education : candidates) {
            List<MatchItemResult> items = evaluate(profile, position, context, education);
            Candidate candidate = new Candidate(education, items);
            if (best == null || candidate.rank() > best.rank()) {
                best = candidate;
            }
        }
        return scopeResult(appendCandidateReason(best.items, best.education), unsupportedScope);
    }

    private List<MatchItemResult> scopeResult(List<MatchItemResult> items, boolean unsupportedScope) {
        if (!unsupportedScope) return items;
        List<MatchItemResult> result = new ArrayList<>(items);
        MatchItemResult major = items.get(1);
        result.set(1, new MatchItemResult(major.getConditionType(), MatchResult.UNCERTAIN,
                major.getUserValue(), major.getRequirementValue(),
                major.getReason() + "；学历阶段或主修、第二学位口径缺少可靠的完整档案证据，需人工核验。", major.getEvidence()));
        return result;
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
