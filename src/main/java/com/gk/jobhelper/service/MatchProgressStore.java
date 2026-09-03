package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.MatchProgressVO;
import com.gk.jobhelper.dto.MatchSummaryVO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MatchProgressStore {
    private final Map<String, MatchProgressVO> tasks = new ConcurrentHashMap<>();

    public MatchProgressVO start(Long profileId, Long importId) {
        MatchProgressVO progress = new MatchProgressVO();
        progress.setStatus("FEATURE_BUILDING");
        tasks.put(key(profileId, importId), progress);
        return progress;
    }

    public MatchProgressVO get(Long profileId, Long importId) { return tasks.get(key(profileId, importId)); }

    public void update(Long profileId, Long importId, MatchSummaryVO summary, String status, String errorMessage) {
        MatchProgressVO progress = tasks.computeIfAbsent(key(profileId, importId), key -> new MatchProgressVO());
        progress.setTotal(summary.getTotal()); progress.setMatch(summary.getMatch());
        progress.setUncertain(summary.getUncertain()); progress.setNotMatch(summary.getNotMatch());
        progress.setFailedCount(summary.getFailedCount());
        progress.setProcessed(summary.getMatch() + summary.getUncertain() + summary.getNotMatch() + summary.getFailedCount());
        progress.setStatus(status); progress.setErrorMessage(errorMessage);
    }

    public void updateFeature(Long profileId, Long importId, int processed, int total) {
        MatchProgressVO progress = tasks.computeIfAbsent(key(profileId, importId), key -> new MatchProgressVO());
        progress.setTotal(total); progress.setMatch(0); progress.setUncertain(0); progress.setNotMatch(0);
        progress.setFailedCount(0); progress.setProcessed(processed); progress.setStatus("FEATURE_BUILDING"); progress.setErrorMessage(null);
    }

    private String key(Long profileId, Long importId) { return profileId + ":" + importId; }
}
