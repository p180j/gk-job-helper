package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.MatchJobRequest;
import com.gk.jobhelper.dto.JobCompareVO;
import com.gk.jobhelper.dto.MatchResultVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.service.JobMatchService;
import com.gk.jobhelper.service.JobCompareService;
import com.gk.jobhelper.service.JobQueryService;
import com.gk.jobhelper.dto.HistoricalAnalysisVO;
import com.gk.jobhelper.service.HistoricalAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 岗位查询接口
 * GET  /api/jobs           分页查询（keyword/departmentName/organizationName/educationRequirement/
 *                           majorRequirement/province/city 过滤）
 * GET  /api/jobs/{id}      岗位详情（含 rawData）
 * POST /api/jobs/{id}/match 单岗位匹配（profileId + 可选 referenceDate，结果落库并返回）
 * GET  /api/jobs/{id}/match 岗位匹配详情（综合结果 + 全部条件明细）
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobQueryService jobQueryService;
    private final JobMatchService jobMatchService;
    private final JobCompareService jobCompareService;
    private final HistoricalAnalysisService historicalAnalysisService;

    public JobController(JobQueryService jobQueryService, JobMatchService jobMatchService,
                         JobCompareService jobCompareService, HistoricalAnalysisService historicalAnalysisService) {
        this.jobQueryService = jobQueryService;
        this.jobMatchService = jobMatchService;
        this.jobCompareService = jobCompareService;
        this.historicalAnalysisService = historicalAnalysisService;
    }

    @GetMapping
    public ApiResponse<PageVO<JobPosition>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "departmentName", required = false) String departmentName,
            @RequestParam(value = "organizationName", required = false) String organizationName,
            @RequestParam(value = "educationRequirement", required = false) String educationRequirement,
            @RequestParam(value = "majorRequirement", required = false) String majorRequirement,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "city", required = false) String city) {
        return ApiResponse.ok(jobQueryService.page(page, size, keyword, departmentName,
                organizationName, educationRequirement, majorRequirement, province, city));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobPosition> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(jobQueryService.getJob(id));
    }

    @GetMapping("/{id}/historical-analysis")
    public ApiResponse<HistoricalAnalysisVO> historical(@PathVariable("id") Long id,
                                                         @RequestParam(value="examYear",defaultValue="2026") Integer examYear) {
        return ApiResponse.ok(historicalAnalysisService.analyze(id, examYear));
    }

    @GetMapping("/compare")
    public ApiResponse<List<JobCompareVO>> compare(@RequestParam("profileId") Long profileId,
                                                    @RequestParam("jobIds") List<Long> jobIds) {
        return ApiResponse.ok(jobCompareService.compare(profileId, jobIds));
    }

    /** 单岗位匹配：执行并保存结果（同档案+岗位重复匹配覆盖更新） */
    @PostMapping("/{id}/match")
    public ApiResponse<MatchResultVO> match(@PathVariable("id") Long id,
                                            @Valid @RequestBody MatchJobRequest request) {
        return ApiResponse.ok(jobMatchService.matchSingle(id, request));
    }

    /** 岗位匹配详情：综合结果 + 全部条件明细（需先执行匹配） */
    @GetMapping("/{id}/match")
    public ApiResponse<MatchResultVO> matchDetail(@PathVariable("id") Long id,
                                                  @RequestParam("profileId") Long profileId) {
        return ApiResponse.ok(jobMatchService.getMatchDetail(id, profileId));
    }
}
