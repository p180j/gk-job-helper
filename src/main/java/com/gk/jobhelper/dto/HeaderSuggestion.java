package com.gk.jobhelper.dto;

import com.gk.jobhelper.constant.FieldConfidence;

/**
 * 单个表头的字段映射建议
 */
public class HeaderSuggestion {

    /** Excel 原始表头字段名 */
    private String sourceField;
    /** 建议映射的标准字段英文名，无法识别时为 null */
    private String suggestedField;
    /** 建议置信度: EXACT / ALIAS / UNKNOWN */
    private FieldConfidence confidence;

    public HeaderSuggestion() {
    }

    public HeaderSuggestion(String sourceField, String suggestedField, FieldConfidence confidence) {
        this.sourceField = sourceField;
        this.suggestedField = suggestedField;
        this.confidence = confidence;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getSuggestedField() {
        return suggestedField;
    }

    public void setSuggestedField(String suggestedField) {
        this.suggestedField = suggestedField;
    }

    public FieldConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(FieldConfidence confidence) {
        this.confidence = confidence;
    }
}
