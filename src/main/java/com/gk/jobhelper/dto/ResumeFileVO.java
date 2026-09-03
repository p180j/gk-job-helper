package com.gk.jobhelper.dto;

import java.time.LocalDateTime;

/** 前端可见的当前简历元信息，绝不暴露服务器存储路径。 */
public class ResumeFileVO {
    private Long resumeId;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
