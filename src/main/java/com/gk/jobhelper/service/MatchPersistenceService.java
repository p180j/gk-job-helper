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
import java.util.List;

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
        LocalDateTime now = LocalDateTime.now();
        Long matchId;

        JobMatch existing = jobMatchMapper.selectByProfileAndPosition(profile.getId(), position.getId());
        if (existing != null) {
            existing.setMatchResult(overall.name());
            existing.setReferenceDate(referenceDate);
            existing.setImportFileId(position.getImportFileId());
            existing.setUpdatedAt(now);
            jobMatchMapper.updateMatch(existing);
            matchId = existing.getId();
            jobMatchMapper.deleteItemsByMatchId(matchId);
        } else {
            JobMatch record = new JobMatch();
            record.setProfileId(profile.getId());
            record.setJobPositionId(position.getId());
            record.setImportFileId(position.getImportFileId());
            record.setMatchResult(overall.name());
            record.setReferenceDate(referenceDate);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            jobMatchMapper.insert(record);
            matchId = record.getId();
        }

        if (items == null || items.isEmpty()) {
            return;
        }
        List<JobMatchItem> entities = new ArrayList<>(items.size());
        for (MatchItemResult item : items) {
            JobMatchItem entity = new JobMatchItem();
            entity.setJobMatchId(matchId);
            entity.setJobPositionId(position.getId());
            entity.setConditionType(item.getConditionType().name());
            entity.setMatchResult(item.getResult().name());
            entity.setUserValue(truncate(item.getUserValue(), 255));
            entity.setRequirementValue(truncate(item.getRequirementValue(), 500));
            entity.setReason(truncate(item.getReason(), 1000));
            entity.setEvidence(toEvidenceJson(item.getEvidence()));
            entity.setCreatedAt(now);
            entities.add(entity);
        }
        jobMatchMapper.insertItems(entities);
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
