package com.gk.jobhelper.controller;
import com.gk.jobhelper.common.ApiResponse;import com.gk.jobhelper.dto.JobPreferenceRequest;import com.gk.jobhelper.service.JobPreferenceService;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/preferences") public class JobPreferenceController {private final JobPreferenceService service;public JobPreferenceController(JobPreferenceService s){service=s;}
 @GetMapping("/{profileId}")public ApiResponse<JobPreferenceRequest> get(@PathVariable Long profileId){return ApiResponse.ok(service.get(profileId));}
 @PutMapping("/{profileId}")public ApiResponse<JobPreferenceRequest> save(@PathVariable Long profileId,@RequestBody JobPreferenceRequest request){return ApiResponse.ok(service.save(profileId,request));}}
