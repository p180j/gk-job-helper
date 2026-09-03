package com.gk.jobhelper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.ai.AiProviderConfig;
import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.CareerProfileDraftVO;
import com.gk.jobhelper.dto.CareerProfileRequest;
import com.gk.jobhelper.dto.CareerProfileVO;
import com.gk.jobhelper.dto.ResumeFileVO;
import com.gk.jobhelper.service.CareerProfileService;
import com.gk.jobhelper.service.ResumeService;
import com.gk.jobhelper.service.ResumeFileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 招聘职业画像：解析仅返回草稿，确认保存才会写入数据库。 */
@RestController
@RequestMapping("/api/career-profile")
public class CareerProfileController {
    private final ResumeService resumeService;
    private final CareerProfileService careerProfileService;
    private final ResumeFileService resumeFileService;
    private final ObjectMapper objectMapper;

    public CareerProfileController(ResumeService resumeService, CareerProfileService careerProfileService,
                                   ResumeFileService resumeFileService, ObjectMapper objectMapper) {
        this.resumeService = resumeService;
        this.careerProfileService = careerProfileService;
        this.resumeFileService = resumeFileService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<CareerProfileVO> getCurrent() {
        return ApiResponse.ok(careerProfileService.getCurrent());
    }

    @GetMapping("/resume")
    public ApiResponse<ResumeFileVO> getCurrentResume() { return ApiResponse.ok(resumeFileService.getCurrent()); }

    @GetMapping("/resume/content")
    public ResponseEntity<FileSystemResource> downloadCurrentResume() {
        com.gk.jobhelper.entity.ResumeFile resume = resumeFileService.currentForDownload();
        MediaType mediaType = "application/pdf".equalsIgnoreCase(resume.getFileType()) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        boolean inline = MediaType.APPLICATION_PDF.equals(mediaType);
        ContentDisposition disposition = inline ? ContentDisposition.inline().filename(resume.getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(resume.getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(resume.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(new FileSystemResource(resume.getStoragePath()));
    }

    @PostMapping("/resume/draft")
    public ApiResponse<CareerProfileDraftVO> createDraft(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("aiConfig") String aiConfigJson) {
        return ApiResponse.ok(resumeService.parse(file, parseAiConfig(aiConfigJson)));
    }

    @PutMapping
    public ApiResponse<CareerProfileVO> save(@RequestBody CareerProfileRequest request) {
        return ApiResponse.ok(careerProfileService.save(request));
    }

    private AiProviderConfig parseAiConfig(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new BusinessException("请先在首页配置并保存 AI 模型，再解析简历。");
        }
        try {
            return objectMapper.readValue(json, AiProviderConfig.class);
        } catch (Exception e) {
            throw new BusinessException("AI 配置格式不正确，请重新保存 AI 模型设置。");
        }
    }
}
