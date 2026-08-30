package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.MatchExecuteRequest;
import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.dto.MatchResultVO;
import com.gk.jobhelper.dto.MatchSummaryVO;
import com.gk.jobhelper.dto.MatchProgressVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.service.JobMatchService;
import com.gk.jobhelper.service.JobMatchAsyncService;
import com.gk.jobhelper.service.MatchProgressStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 匹配接口
 * POST /api/match/execute   批量匹配：对指定导入批次下所有岗位执行匹配（分页处理）
 * GET  /api/match/result    匹配结果分页查询（profileId + importId + result 过滤）
 */
@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final JobMatchService jobMatchService;
    private final JobMatchAsyncService jobMatchAsyncService;
    private final MatchProgressStore matchProgressStore;

    public MatchController(JobMatchService jobMatchService, JobMatchAsyncService jobMatchAsyncService,
                           MatchProgressStore matchProgressStore) {
        this.jobMatchService = jobMatchService;
        this.jobMatchAsyncService = jobMatchAsyncService;
        this.matchProgressStore = matchProgressStore;
    }

    @PostMapping("/execute")
    public ApiResponse<MatchSummaryVO> execute(@Valid @RequestBody MatchExecuteRequest request) {
        return ApiResponse.ok(jobMatchService.batchExecute(request));
    }

    @PostMapping("/execute-async")
    public ApiResponse<MatchProgressVO> executeAsync(@Valid @RequestBody MatchExecuteRequest request) {
        MatchProgressVO progress = matchProgressStore.start(request.getProfileId(), request.getImportId());
        jobMatchAsyncService.execute(request);
        return ApiResponse.ok(progress);
    }

    @GetMapping("/progress")
    public ApiResponse<MatchProgressVO> progress(@RequestParam("profileId") Long profileId,
                                                  @RequestParam("importId") Long importId) {
        return ApiResponse.ok(matchProgressStore.get(profileId, importId));
    }

    @GetMapping("/result")
    public ApiResponse<PageVO<MatchPositionResultVO>> result(
            @RequestParam("profileId") Long profileId,
            @RequestParam(value = "importId", required = false) Long importId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "result", required = false) String legacyResult,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "organizationKeyword", required = false) String organizationKeyword,
            @RequestParam(value = "positionKeyword", required = false) String positionKeyword,
            @RequestParam(value = "recruitCountMin", required = false) Integer recruitCountMin,
            @RequestParam(value = "recruitCountMax", required = false) Integer recruitCountMax,
            @RequestParam(value = "educationKeyword", required = false) String educationKeyword,
            @RequestParam(value = "majorKeyword", required = false) String majorKeyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String effectiveStatus = status == null || status.trim().isEmpty() ? legacyResult : status;
        return ApiResponse.ok(jobMatchService.queryResults(profileId, importId, effectiveStatus,
                region, organizationKeyword, positionKeyword, recruitCountMin, recruitCountMax,
                educationKeyword, majorKeyword, safePage, safeSize));
    }

    @GetMapping("/regions")
    public ApiResponse<List<String>> regions(@RequestParam("profileId") Long profileId,
                                              @RequestParam("importId") Long importId) {
        return ApiResponse.ok(jobMatchService.queryResultRegions(profileId, importId));
    }
}
