package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.MatchExecuteRequest;
import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.dto.MatchResultVO;
import com.gk.jobhelper.dto.MatchSummaryVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.service.JobMatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 匹配接口
 * POST /api/match/execute   批量匹配：对指定导入批次下所有岗位执行匹配（分页处理）
 * GET  /api/match/result    匹配结果分页查询（profileId + importId + result 过滤）
 */
@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final JobMatchService jobMatchService;

    public MatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    @PostMapping("/execute")
    public ApiResponse<MatchSummaryVO> execute(@Valid @RequestBody MatchExecuteRequest request) {
        return ApiResponse.ok(jobMatchService.batchExecute(request));
    }

    @GetMapping("/result")
    public ApiResponse<PageVO<MatchPositionResultVO>> result(
            @RequestParam("profileId") Long profileId,
            @RequestParam(value = "importId", required = false) Long importId,
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return ApiResponse.ok(jobMatchService.queryResults(profileId, importId, result, safePage, safeSize));
    }
}
