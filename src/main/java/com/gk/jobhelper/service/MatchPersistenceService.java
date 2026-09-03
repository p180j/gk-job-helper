package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobMatch;
import com.gk.jobhelper.entity.JobMatchItem;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.JobMatchMapper;
import com.gk.jobhelper.matcher.MatchEvidence;
import com.gk.jobhelper.matcher.MatchItemResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 匹配结果持久化服务：独立事务保存 job_match + job_match_item。
 * 同一档案+岗位已存在匹配记录时覆盖更新（删旧明细再插入），避免无限生成重复记录。
 * Iteration 4 起：MAJOR 等条件的结构化证据以 JSON 形式保存于 job_match_item.evidence。
 */
@Service
public class MatchPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(MatchPersistenceService.class);

    private final JobMatchMapper jobMatchMapper;
    private final ObjectMapper objectMapper;

    public MatchPersistenceService(JobMatchMapper jobMatchMapper, ObjectMapper objectMapper) {
        this.jobMatchMapper = jobMatchMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persist(UserProfile profile, JobPosition position, MatchResult overall,
                        LocalDate referenceDate, List<MatchItemResult> items) {
        persistBatch(profile, Collections.singletonList(new PendingMatch(position, overall, referenceDate, items)));
    }

    /** 同一批岗位仅用一次事务完成汇总 upsert 和条件明细替换，避免逐岗位事务往返。 */
    @Transactional
    public void persistBatch(UserProfile profile, List<PendingMatch> pendingMatches) {
        if (pendingMatches == null || pendingMatches.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<JobMatch> records = new ArrayList<>(pendingMatches.size());
        List<Long> positionIds = new ArrayList<>(pendingMatches.size());
        for (PendingMatch pending : pendingMatches) {
            JobMatch record = new JobMatch();
            record.setProfileId(profile.getId());
            record.setJobPositionId(pending.position.getId());
            record.setImportFileId(pending.position.getImportFileId());
            record.setMatchResult(pending.overall.name());
            record.setReferenceDate(pending.referenceDate);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
            positionIds.add(record.getJobPositionId());
        }
        jobMatchMapper.upsertBatch(records);
        Map<Long, Long> matchIds = new HashMap<>();
        List<Long> persistedIds = new ArrayList<>();
        for (JobMatch record : jobMatchMapper.selectByProfileAndPositionIds(profile.getId(), positionIds)) {
            matchIds.put(record.getJobPositionId(), record.getId());
            persistedIds.add(record.getId());
        }
        if (matchIds.size() != pendingMatches.size()) {
            throw new IllegalStateException("批量保存匹配结果后未能读取完整结果");
        }
        jobMatchMapper.deleteItemsByMatchIds(persistedIds);
        List<JobMatchItem> entities = new ArrayList<>();
        for (PendingMatch pending : pendingMatches) {
            if (pending.items == null) continue;
            for (MatchItemResult item : pending.items) {
                JobMatchItem entity = new JobMatchItem();
                entity.setJobMatchId(matchIds.get(pending.position.getId()));
                entity.setJobPositionId(pending.position.getId());
                entity.setConditionType(item.getConditionType().name());
                entity.setMatchResult(item.getResult().name());
                entity.setUserValue(truncate(item.getUserValue(), 255));
                entity.setRequirementValue(truncate(item.getRequirementValue(), 500));
                entity.setReason(truncate(item.getReason(), 1000));
                entity.setEvidence(toEvidenceJson(item.getEvidence()));
                entity.setCreatedAt(now);
                entities.add(entity);
            }
        }
        jobMatchMapper.insertItems(entities);
    }

    public static class PendingMatch {
        private final JobPosition position;
        private final MatchResult overall;
        private final LocalDate referenceDate;
        private final List<MatchItemResult> items;
        public PendingMatch(JobPosition position, MatchResult overall, LocalDate referenceDate, List<MatchItemResult> items) {
            this.position = position; this.overall = overall; this.referenceDate = referenceDate; this.items = items;
        }
        public MatchResult getOverall() { return overall; }
        public LocalDate getReferenceDate() { return referenceDate; }
        public List<MatchItemResult> getItems() { return items; }
    }

    /** 结构化证据序列化为 JSON（VARCHAR/TEXT 保存，MySQL 兼容）；序列化失败时仅丢失证据不影响 reason */
    private String toEvidenceJson(MatchEvidence evidence) {
        if (evidence == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(evidence), 1000);
        } catch (Exception e) {
            log.warn("匹配证据序列化失败，仅保存 reason: {}", e.getMessage());
            return null;
        }
    }

    /** 防止超长文本超出列宽 */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
