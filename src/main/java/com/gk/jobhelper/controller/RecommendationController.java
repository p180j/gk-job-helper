package com.gk.jobhelper.controller;
import com.gk.jobhelper.common.ApiResponse;import com.gk.jobhelper.dto.*;import com.gk.jobhelper.service.RecommendationService;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/recommendations") public class RecommendationController {private final RecommendationService service;public RecommendationController(RecommendationService s){service=s;}
 @GetMapping public ApiResponse<PageVO<RecommendationItemVO>> page(@RequestParam Long profileId,@RequestParam Long importId,@RequestParam(required=false)String priorityLevel,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(service.page(profileId,importId,priorityLevel,page,size));}}
