package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.MatchExecuteRequest;
import com.gk.jobhelper.dto.MatchSummaryVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class JobMatchAsyncService {
    private final JobMatchService jobMatchService;
    private final MatchProgressStore progressStore;

    public JobMatchAsyncService(JobMatchService jobMatchService, MatchProgressStore progressStore) {
        this.jobMatchService = jobMatchService; this.progressStore = progressStore;
    }

    @Async("matchExecutor")
    public void execute(MatchExecuteRequest request) {
        try {
            MatchSummaryVO summary = jobMatchService.batchExecute(request,
                    current -> progressStore.update(request.getProfileId(), request.getImportId(), current, "MATCHING", null));
            progressStore.update(request.getProfileId(), request.getImportId(), summary, "COMPLETED", null);
        } catch (Exception e) {
            MatchSummaryVO empty = new MatchSummaryVO();
            progressStore.update(request.getProfileId(), request.getImportId(), empty, "FAILED",
                    e.getMessage() == null ? "匹配任务失败" : e.getMessage());
        }
    }
}
