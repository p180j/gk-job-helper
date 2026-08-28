package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.config.MatchProperties;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.dto.MatchExecuteRequest;
import com.gk.jobhelper.dto.MatchJobRequest;
import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.dto.MatchResultQuery;
import com.gk.jobhelper.dto.MatchResultVO;
import com.gk.jobhelper.dto.MatchSummaryVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobMatch;
import com.gk.jobhelper.entity.JobMatchItem;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobMatchMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import com.gk.jobhelper.matcher.JobConditionMatcher;
import com.gk.jobhelper.matcher.MatchContext;
import com.gk.jobhelper.matcher.MatchEvidence;
import com.gk.jobhelper.matcher.MatchItemResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 报考资格匹配引擎 V1 编排服务：
 * - 单岗位匹配 / 批量匹配（按导入批次分页处理，不整批加载进内存）
 * - 综合结果聚合（NOT_MATCH > UNCERTAIN > MATCH，禁止评分/概率）
 * - 匹配结果分页查询 / 岗位匹配详情
 */
@Service
public class JobMatchService {

    private static final Logger log = LoggerFactory.getLogger(JobMatchService.class);

    private final UserProfileMapper userProfileMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ImportFileMapper importFileMapper;
    private final JobMatchMapper jobMatchMapper;
    private final MatchPersistenceService matchPersistenceService;
    private final MatchProperties matchProperties;
    private final ObjectMapper objectMapper;
    private final QualificationEducationResolver qualificationEducationResolver;
    /** 已注册的条件匹配器（按 ConditionType 声明顺序执行） */
    private final List<JobConditionMatcher> matchers;

    public JobMatchService(UserProfileMapper userProfileMapper,
                           JobPositionMapper jobPositionMapper,
                           ImportFileMapper importFileMapper,
                           JobMatchMapper jobMatchMapper,
                           MatchPersistenceService matchPersistenceService,
                           MatchProperties matchProperties,
                           ObjectMapper objectMapper,
                           QualificationEducationResolver qualificationEducationResolver,
                           List<JobConditionMatcher> matchers) {
        this.userProfileMapper = userProfileMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.importFileMapper = importFileMapper;
        this.jobMatchMapper = jobMatchMapper;
        this.matchPersistenceService = matchPersistenceService;
        this.matchProperties = matchProperties;
        this.objectMapper = objectMapper;
        this.qualificationEducationResolver = qualificationEducationResolver;
        // 固定输出顺序，避免 Spring 注入顺序不稳定导致 items 顺序漂移
        List<JobConditionMatcher> sorted = new ArrayList<>(matchers);
        sorted.sort(Comparator.comparing(matcher -> matcher.support().ordinal()));
        this.matchers = sorted;
    }

    /**
     * 单岗位匹配（结果落库并返回）
     */
    public MatchResultVO matchSingle(Long jobId, MatchJobRequest request) {
        UserProfile profile = requireProfile(request.getProfileId());
        JobPosition position = requireJob(jobId);
        return executeMatch(profile, position, request.getReferenceDate());
    }

    /**
     * 批量匹配：对 importId 下所有岗位执行匹配，分页加载（默认每批 200，可配置）。
     * 单个岗位异常不中断批次，记录失败岗位与原因。
     */
    public MatchSummaryVO batchExecute(MatchExecuteRequest request) {
        UserProfile profile = requireProfile(request.getProfileId());
        requireImportFile(request.getImportId());

        long total = jobPositionMapper.countByImportFileId(request.getImportId());
        MatchSummaryVO summary = new MatchSummaryVO();
        summary.setTotal(total);

        int batchSize = Math.max(1, matchProperties.getBatchSize());
        int maxFailedItems = Math.max(0, matchProperties.getMaxFailedItems());

        for (int offset = 0; ; offset += batchSize) {
            List<JobPosition> positions =
                    jobPositionMapper.selectPageByImportFileId(request.getImportId(), offset, batchSize);
            if (positions == null || positions.isEmpty()) {
                break;
            }
            for (JobPosition position : positions) {
                try {
                    MatchResultVO result = executeMatch(profile, position, request.getReferenceDate());
                    incrementSummary(summary, result.getResult());
                } catch (Exception e) {
                    // 单岗位失败不影响整个批次
                    log.warn("批量匹配单岗位失败, jobId={}, reason={}", position.getId(), e.getMessage());
                    summary.setFailedCount(summary.getFailedCount() + 1);
                    if (summary.getFailedItems().size() < maxFailedItems) {
                        summary.getFailedItems().add(new MatchSummaryVO.FailedItem(
                                position.getId(), "匹配执行异常: " + e.getMessage()));
                    }
                }
            }
            if (positions.size() < batchSize) {
                break;
            }
        }
        return summary;
    }

    /**
     * 匹配结果分页查询（可按 profileId + importId + result 过滤）
     */
    public PageVO<MatchPositionResultVO> queryResults(Long profileId, Long importId, String result,
                                                      int page, int size) {
        if (profileId == null) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "profileId 不能为空");
        }
        MatchResult resultFilter = parseResultFilter(result);

        MatchResultQuery query = new MatchResultQuery();
        query.setProfileId(profileId);
        query.setImportFileId(importId);
        query.setMatchResult(resultFilter == null ? null : resultFilter.name());

        long total = jobMatchMapper.countResultPage(query);
        query.setOffset((page - 1) * size);
        query.setSize(size);
        List<MatchPositionResultVO> items = jobMatchMapper.selectResultPage(query);
        return new PageVO<>(total, page, size, items);
    }

    /**
     * 岗位匹配详情：综合结果 + 全部条件明细（用于解释为什么能报/不能报/需确认）
     */
    public MatchResultVO getMatchDetail(Long jobId, Long profileId) {
        if (profileId == null) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "profileId 不能为空");
        }
        requireJob(jobId);
        JobMatch match = jobMatchMapper.selectByProfileAndPosition(profileId, jobId);
        if (match == null) {
            throw new BusinessException(ApiResponse.CODE_MATCH_NOT_FOUND,
                    "匹配结果不存在，请先执行匹配: jobId=" + jobId + ", profileId=" + profileId);
        }
        List<JobMatchItem> entities = jobMatchMapper.selectItemsByMatchId(match.getId());
        List<MatchItemResult> items = new ArrayList<>(entities.size());
        for (JobMatchItem entity : entities) {
            items.add(new MatchItemResult(
                    ConditionType.valueOf(entity.getConditionType()),
                    MatchResult.valueOf(entity.getMatchResult()),
                    entity.getUserValue(),
                    entity.getRequirementValue(),
                    entity.getReason(),
                    parseEvidence(entity.getEvidence())));
        }
        MatchResultVO vo = new MatchResultVO();
        vo.setJobId(jobId);
        vo.setProfileId(profileId);
        vo.setResult(match.getMatchResult());
        vo.setReferenceDate(match.getReferenceDate());
        vo.setItems(items);
        return vo;
    }

    /**
     * 综合结果聚合（固定规则，禁止评分/概率/匹配度）:
     * 存在 NOT_MATCH -> NOT_MATCH；否则存在 UNCERTAIN -> UNCERTAIN；否则 MATCH
     */
    public static MatchResult aggregate(List<MatchItemResult> items) {
        boolean hasUncertain = false;
        for (MatchItemResult item : items) {
            if (item.getResult() == MatchResult.NOT_MATCH) {
                return MatchResult.NOT_MATCH;
            }
            if (item.getResult() == MatchResult.UNCERTAIN) {
                hasUncertain = true;
            }
        }
        return hasUncertain ? MatchResult.UNCERTAIN : MatchResult.MATCH;
    }

    /** 依次执行全部 Matcher -> 聚合 -> 持久化 -> 返回 VO */
    private MatchResultVO executeMatch(UserProfile profile, JobPosition position, LocalDate referenceDate) {
        MatchContext context = MatchContext.of(referenceDate);
        List<MatchItemResult> items = new ArrayList<>(matchers.size());
        List<MatchItemResult> qualificationItems = qualificationEducationResolver.resolve(profile, position, context);
        items.add(qualificationItems.get(0));
        for (JobConditionMatcher matcher : matchers) {
            if (matcher.support() == ConditionType.EDUCATION || matcher.support() == ConditionType.MAJOR) {
                continue;
            }
            items.add(matcher.match(profile, position, context));
        }
        items.add(qualificationItems.get(1));
        MatchResult overall = aggregate(items);
        matchPersistenceService.persist(profile, position, overall, context.getReferenceDate(), items);

        MatchResultVO vo = new MatchResultVO();
        vo.setJobId(position.getId());
        vo.setProfileId(profile.getId());
        vo.setResult(overall.name());
        vo.setReferenceDate(context.getReferenceDate());
        vo.setItems(items);
        return vo;
    }

    private void incrementSummary(MatchSummaryVO summary, String result) {
        if (MatchResult.MATCH.name().equals(result)) {
            summary.setMatch(summary.getMatch() + 1);
        } else if (MatchResult.UNCERTAIN.name().equals(result)) {
            summary.setUncertain(summary.getUncertain() + 1);
        } else {
            summary.setNotMatch(summary.getNotMatch() + 1);
        }
    }

    private MatchResult parseResultFilter(String result) {
        if (result == null || result.trim().isEmpty()) {
            return null;
        }
        String trimmed = result.trim();
        for (MatchResult value : MatchResult.values()) {
            if (value.name().equals(trimmed)) {
                return value;
            }
        }
        throw new BusinessException(ApiResponse.CODE_BAD_REQUEST,
                "非法的 result 过滤值: " + trimmed + "，必须为 MATCH / UNCERTAIN / NOT_MATCH");
    }

    /** 反序列化匹配证据 JSON；解析失败时返回 null（reason 仍可读） */
    private MatchEvidence parseEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(evidenceJson, MatchEvidence.class);
        } catch (Exception e) {
            log.warn("匹配证据反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private UserProfile requireProfile(Long profileId) {
        UserProfile profile = userProfileMapper.selectById(profileId);
        if (profile == null) {
            throw new BusinessException(ApiResponse.CODE_PROFILE_NOT_FOUND, "档案不存在: id=" + profileId);
        }
        return profile;
    }

    private JobPosition requireJob(Long jobId) {
        JobPosition position = jobPositionMapper.selectById(jobId);
        if (position == null) {
            throw new BusinessException(ApiResponse.CODE_JOB_NOT_FOUND, "岗位不存在: id=" + jobId);
        }
        return position;
    }

    private void requireImportFile(Long importId) {
        ImportFile record = importFileMapper.selectById(importId);
        if (record == null) {
            throw new BusinessException(ApiResponse.CODE_IMPORT_NOT_FOUND, "导入记录不存在: id=" + importId);
        }
    }
}
