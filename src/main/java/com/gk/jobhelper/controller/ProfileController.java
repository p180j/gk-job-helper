package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.ProfileRequest;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 个人档案接口
 * GET  /api/profile  查询档案
 * POST /api/profile  创建档案
 * PUT  /api/profile  更新档案
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfile> getProfile() {
        return ApiResponse.ok(userProfileService.getProfile());
    }

    @PostMapping
    public ApiResponse<UserProfile> createProfile(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(userProfileService.createProfile(request));
    }

    @PutMapping
    public ApiResponse<UserProfile> updateProfile(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(userProfileService.updateProfile(request));
    }
}
