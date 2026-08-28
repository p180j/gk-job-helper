package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/**
 * 考试与专业目录绑定（一个考试可绑定多个目录，priority 越小优先级越高）
 */
public class ExamMajorCatalog {

    private Long id;
    /** 考试 -> exam.id */
    private Long examId;
    /** 目录 -> major_catalog.id */
    private Long catalogId;
    /** 绑定优先级：数字越小优先级越高 */
    private Integer priority;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(Long catalogId) {
        this.catalogId = catalogId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
