package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/**
 * 专业目录节点（父子树结构，专业类 -> 专业的归属关系全部由 parent_id 表达，
 * 禁止在 Java 代码中硬编码专业归属）
 */
public class MajorCatalogItem {

    private Long id;
    /** 所属目录 -> major_catalog.id */
    private Long catalogId;
    /** 父节点 -> major_catalog_item.id（根节点为 null） */
    private Long parentId;
    /** 专业/类代码，如 080902 / 0809 / 0812（不假设固定位数） */
    private String majorCode;
    /** 节点名称 */
    private String majorName;
    /** 标准化名称（比较用） */
    private String normalizedName;
    /** CATEGORY / CLASS / MAJOR / DISCIPLINE / FIELD / OTHER，见 CatalogItemLevel */
    private String itemLevel;
    /** 学位类别（研究生目录用：学术学位/专业学位） */
    private String degreeCategory;
    /** 排序号 */
    private Integer sortNo;
    /** 原始行 JSON（来源导入时保留） */
    private String rawData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogId() {
        return catalogId;
    }

    public void setCatalogId(Long catalogId) {
        this.catalogId = catalogId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMajorCode() {
        return majorCode;
    }

    public void setMajorCode(String majorCode) {
        this.majorCode = majorCode;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getItemLevel() {
        return itemLevel;
    }

    public void setItemLevel(String itemLevel) {
        this.itemLevel = itemLevel;
    }

    public String getDegreeCategory() {
        return degreeCategory;
    }

    public void setDegreeCategory(String degreeCategory) {
        this.degreeCategory = degreeCategory;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
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
