package com.gk.jobhelper.entity;

import java.time.LocalDateTime;

/**
 * 专业别名（仅允许来源明确或人工维护的别名，禁止自动相似推断）
 */
public class MajorAlias {

    private Long id;
    /** 所属目录 -> major_catalog.id */
    private Long catalogId;
    /** 指向目录节点 -> major_catalog_item.id */
    private Long majorCatalogItemId;
    /** 别名原始值 */
    private String aliasName;
    /** 标准化别名（比较用） */
    private String normalizedAlias;
    /** OFFICIAL / MANUAL，见 AliasType */
    private String aliasType;
    private LocalDateTime createdAt;

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

    public Long getMajorCatalogItemId() {
        return majorCatalogItemId;
    }

    public void setMajorCatalogItemId(Long majorCatalogItemId) {
        this.majorCatalogItemId = majorCatalogItemId;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getNormalizedAlias() {
        return normalizedAlias;
    }

    public void setNormalizedAlias(String normalizedAlias) {
        this.normalizedAlias = normalizedAlias;
    }

    public String getAliasType() {
        return aliasType;
    }

    public void setAliasType(String aliasType) {
        this.aliasType = aliasType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
