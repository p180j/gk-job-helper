package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.config.UploadProperties;
import com.gk.jobhelper.dto.FeatureRebuildResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.JobPositionFeature;
import com.gk.jobhelper.mapper.JobPositionFeatureMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Durable local snapshots for parsed fields; original position rows are never rewritten. */
@Service
public class JobFeatureSnapshotService {
    private final UploadProperties uploadProperties;
    private final ObjectMapper objectMapper;
    private final JobPositionMapper positionMapper;
    private final JobPositionFeatureMapper featureMapper;

    public JobFeatureSnapshotService(UploadProperties uploadProperties, ObjectMapper objectMapper,
                                      JobPositionMapper positionMapper, JobPositionFeatureMapper featureMapper) {
        this.uploadProperties = uploadProperties;
        this.objectMapper = objectMapper;
        this.positionMapper = positionMapper;
        this.featureMapper = featureMapper;
    }

    public String create(Long importId, List<JobPosition> positions) {
        Snapshot snapshot = new Snapshot();
        snapshot.setImportId(importId);
        snapshot.setParserVersion(JobRemarkParser.PARSER_VERSION);
        for (JobPosition position : positions) snapshot.getPositionIds().add(position.getId());
        for (int start = 0; start < snapshot.getPositionIds().size(); start += 200) {
            List<Long> ids = snapshot.getPositionIds().subList(start, Math.min(start + 200, snapshot.getPositionIds().size()));
            snapshot.getFeatures().addAll(featureMapper.selectByPositionIds(ids));
        }
        String backupId = UUID.randomUUID().toString();
        Path destination = path(importId, backupId);
        Path temporary = destination.resolveSibling(backupId + ".tmp");
        try {
            Files.createDirectories(destination.getParent());
            objectMapper.writeValue(temporary.toFile(), snapshot);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination);
            }
            return backupId;
        } catch (Exception e) {
            throw new BusinessException("重建前快照保存失败，已停止重建：" + e.getMessage());
        }
    }

    @Transactional
    public FeatureRebuildResult restore(Long importId, String backupId) {
        Snapshot snapshot;
        try {
            snapshot = objectMapper.readValue(path(importId, backupId).toFile(), Snapshot.class);
        } catch (Exception e) {
            throw new BusinessException("派生特征快照不可读：" + e.getMessage());
        }
        if (snapshot.getPositionIds() == null || snapshot.getFeatures() == null) {
            throw new BusinessException("派生特征快照内容不完整，已停止恢复");
        }
        Set<Long> liveIds = new HashSet<>();
        for (JobPosition position : positionMapper.selectByImportFileId(importId)) liveIds.add(position.getId());
        if (!Objects.equals(importId, snapshot.getImportId())
                || !liveIds.equals(new HashSet<>(snapshot.getPositionIds()))) {
            throw new BusinessException("快照与当前导入批次的岗位集合不一致，已停止恢复");
        }
        Map<Long, JobPositionFeature> originals = new HashMap<>();
        for (JobPositionFeature feature : snapshot.getFeatures()) {
            if (!liveIds.contains(feature.getPositionId())) throw new BusinessException("快照包含其他批次的岗位，已停止恢复");
            originals.put(feature.getPositionId(), feature);
        }
        FeatureRebuildResult result = new FeatureRebuildResult();
        result.setImportId(importId);
        result.setBackupId(backupId);
        result.setTotal(liveIds.size());
        for (Long positionId : liveIds) {
            JobPositionFeature original = originals.get(positionId);
            JobPositionFeature current = featureMapper.selectByPositionId(positionId);
            if (!sameParsedFields(current, original)) {
                if (original == null) featureMapper.deleteByPositionId(positionId);
                else {
                    original.setUpdatedAt(LocalDateTime.now());
                    if (current == null) featureMapper.upsert(original);
                    else featureMapper.updateParsedFields(original);
                }
                result.setChanged(result.getChanged() + 1);
            }
            result.setSuccess(result.getSuccess() + 1);
        }
        return result;
    }

    public static boolean sameParsedFields(JobPositionFeature left, JobPositionFeature right) {
        if (left == null || right == null) return left == right;
        return Objects.equals(left.getEffectiveMajorRequirement(), right.getEffectiveMajorRequirement())
                && Objects.equals(left.getRemarkConditions(), right.getRemarkConditions())
                && Objects.equals(left.getWorkLocationName(), right.getWorkLocationName())
                && Objects.equals(left.getWorkLocationCode(), right.getWorkLocationCode())
                && Objects.equals(left.getWorkLocationStatus(), right.getWorkLocationStatus())
                && Objects.equals(left.getWorkLocationSource(), right.getWorkLocationSource())
                && Objects.equals(left.getWorkLocationEvidence(), right.getWorkLocationEvidence())
                && Objects.equals(left.getWorkLocationType(), right.getWorkLocationType());
    }

    private Path path(Long importId, String backupId) {
        if (importId == null || importId <= 0 || backupId == null
                || !backupId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new BusinessException("无效的批次或快照编号");
        }
        return Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize()
                .resolve("feature-backups").resolve(importId.toString()).resolve(backupId + ".json");
    }

    public static class Snapshot {
        private Long importId;
        private String parserVersion;
        private List<Long> positionIds = new ArrayList<>();
        private List<JobPositionFeature> features = new ArrayList<>();
        public Long getImportId() { return importId; }
        public void setImportId(Long value) { importId = value; }
        public String getParserVersion() { return parserVersion; }
        public void setParserVersion(String value) { parserVersion = value; }
        public List<Long> getPositionIds() { return positionIds; }
        public void setPositionIds(List<Long> value) { positionIds = value; }
        public List<JobPositionFeature> getFeatures() { return features; }
        public void setFeatures(List<JobPositionFeature> value) { features = value; }
    }
}
