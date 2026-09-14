package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.RecruitmentSourceCreateRequest;
import com.gk.jobhelper.entity.RecruitmentSource;
import com.gk.jobhelper.service.RecruitmentSourceService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruitment/sources")
public class RecruitmentSourceController {
    private final RecruitmentSourceService sources;

    public RecruitmentSourceController(RecruitmentSourceService sources) {
        this.sources = sources;
    }

    @PostMapping
    public ApiResponse<RecruitmentSource> create(@Valid @RequestBody RecruitmentSourceCreateRequest request) {
        return ApiResponse.ok(sources.create(request));
    }
}
