package com.gk.jobhelper.controller;
import com.gk.jobhelper.common.ApiResponse; import com.gk.jobhelper.dto.*; import com.gk.jobhelper.service.JobFeatureService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/job-features") public class JobFeatureController {
 private final JobFeatureService service; public JobFeatureController(JobFeatureService s){service=s;}
 @PostMapping("/rebuild") public ApiResponse<FeatureRebuildResult> rebuild(@RequestParam("importId")Long importId){return ApiResponse.ok(service.rebuild(importId));}
 @GetMapping("/{positionId}") public ApiResponse<JobFeatureVO> get(@PathVariable Long positionId){return ApiResponse.ok(service.get(positionId));}
}
