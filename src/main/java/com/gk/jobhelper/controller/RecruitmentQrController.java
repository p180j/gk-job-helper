package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.service.RecruitmentNoticeService;
import com.gk.jobhelper.service.RecruitmentQrResolver;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruitment/notices")
public class RecruitmentQrController {
    private final RecruitmentNoticeService notices;
    private final RecruitmentQrResolver qrResolver;

    public RecruitmentQrController(RecruitmentNoticeService notices, RecruitmentQrResolver qrResolver) {
        this.notices = notices;
        this.qrResolver = qrResolver;
    }

    @PostMapping("/{id}/resolve-qr-attachments")
    public ApiResponse<RecruitmentQrResolver.Resolution> resolveQrAttachments(@PathVariable Long id) {
        notices.detail(id);
        return ApiResponse.ok(qrResolver.resolve(id));
    }
}
