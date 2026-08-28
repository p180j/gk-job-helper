package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/**
 * 专业目录（可版本化、可追溯、可扩展）
 */
public class MajorCatalog {

    private Long id;
    /** 目录编码，如 MOE_UNDERGRADUATE_2026 */
    private String catalogCode;
    /** 目录名称，如 普通高等学校本科专业目录(2026年) */
    private String catalogName;
    /** MOE / EXAM / AGENCY / CUSTOM，见 CatalogType */
    private String catalogType;
    /** UNDERGRADUATE / GRADUATE / VOCATIONAL / MIXED，见 MajorEducationLevel */
    private String educationLevel;
    /** 目录版本(年份等) */
    private String version;
    /** 来源单位名称 */
    private String sourceName;
    /** 来源官方链接 */
    private String sourceUrl;
    /** 来源年份 */
    private String sourceYear;
    /** 优先级：数字越小优先级越高 */
    private Integer priority;
    /** 是否启用 */
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCatalogCode() {
        return catalogCode;
    }

    public void setCatalogCode(String catalogCode) {
        this.catalogCode = catalogCode;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public String getCatalogType() {
        return catalogType;
    }

    public void setCatalogType(String catalogType) {
        this.catalogType = catalogType;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceYear() {
        return sourceYear;
    }

    public void setSourceYear(String sourceYear) {
        this.sourceYear = sourceYear;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
