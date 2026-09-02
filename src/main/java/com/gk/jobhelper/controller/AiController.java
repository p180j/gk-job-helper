package com.gk.jobhelper.controller;

import com.gk.jobhelper.ai.AiProviderConfig;
import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.AiTestResult;
import com.gk.jobhelper.service.AiTestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiTestService aiTestService;

    public AiController(AiTestService aiTestService) {
        this.aiTestService = aiTestService;
    }

    @PostMapping("/test")
    public ApiResponse<AiTestResult> test(@Valid @RequestBody AiProviderConfig config) {
        return ApiResponse.ok(aiTestService.test(config));
    }
}
