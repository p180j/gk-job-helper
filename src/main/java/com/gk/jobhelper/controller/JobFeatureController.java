package com.gk.jobhelper.controller;
import com.gk.jobhelper.common.ApiResponse; import com.gk.jobhelper.dto.*; import com.gk.jobhelper.service.JobFeatureService;
import org.springframework.web.bind.annotation.*;
import com.gk.jobhelper.service.JobFeatureSnapshotService;
@RestController @RequestMapping("/api/job-features") public class JobFeatureController {
 private final JobFeatureService service; private final JobFeatureSnapshotService snapshots;
 public JobFeatureController(JobFeatureService s,JobFeatureSnapshotService snapshots){service=s;this.snapshots=snapshots;}
 @PostMapping("/rebuild") public ApiResponse<FeatureRebuildResult> rebuild(@RequestParam("importId")Long importId){return ApiResponse.ok(service.rebuild(importId));}
 @GetMapping("/parsed-preview") public ApiResponse<FeatureRebuildResult> previewParsed(@RequestParam("importId")Long importId){return ApiResponse.ok(service.previewParsed(importId));}
 @PostMapping("/parsed-rebuild") public ApiResponse<FeatureRebuildResult> rebuildParsed(@RequestParam("importId")Long importId){return ApiResponse.ok(service.applyParsed(importId));}
 @PostMapping("/parsed-restore") public ApiResponse<FeatureRebuildResult> restoreParsed(@RequestParam("importId")Long importId,@RequestParam("backupId")String backupId){return ApiResponse.ok(snapshots.restore(importId,backupId));}
 @GetMapping("/{positionId}") public ApiResponse<JobFeatureVO> get(@PathVariable Long positionId){return ApiResponse.ok(service.get(positionId));}
}
