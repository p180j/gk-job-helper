package com.gk.jobhelper.service;

import com.gk.jobhelper.constant.FieldConfidence;
import com.gk.jobhelper.constant.PositionStandardField;
import com.gk.jobhelper.dto.HeaderSuggestion;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 表头与标准字段映射建议服务（纯字典匹配，不做 AI 猜测）
 */
@Component
public class FieldMappingService {

    /**
     * 对单个表头给出映射建议
     */
    public HeaderSuggestion suggest(String sourceHeader) {
        String raw = sourceHeader == null ? "" : sourceHeader;
        String normalized = PositionStandardField.normalize(sourceHeader);
        if (normalized.isEmpty()) {
            return new HeaderSuggestion(raw, null, FieldConfidence.UNKNOWN);
        }
        // 1. 精确匹配优先
        for (PositionStandardField field : PositionStandardField.values()) {
            if (field.matchesExact(normalized)) {
                return new HeaderSuggestion(raw, field.getFieldName(), FieldConfidence.EXACT);
            }
        }
        // 2. 同义词匹配其次
        for (PositionStandardField field : PositionStandardField.values()) {
            if (field.matchesAlias(normalized)) {
                return new HeaderSuggestion(raw, field.getFieldName(), FieldConfidence.ALIAS);
            }
        }
        // 3. 无法识别
        return new HeaderSuggestion(raw, null, FieldConfidence.UNKNOWN);
    }

    /**
     * 对全部表头按原顺序给出映射建议
     */
    public List<HeaderSuggestion> suggestAll(List<String> headers) {
        List<HeaderSuggestion> suggestions = new ArrayList<>();
        if (headers == null) {
            return suggestions;
        }
        for (String header : headers) {
            suggestions.add(suggest(header));
        }
        return suggestions;
    }

    /**
     * 目标字段是否为合法标准字段英文名
     */
    public boolean isKnownTargetField(String targetField) {
        return PositionStandardField.byFieldName(targetField) != null;
    }

    /**
     * 在表头中查找 sourceField 对应的列下标（归一化比较），找不到返回 -1
     */
    public int findColumnIndex(List<String> headers, String sourceField) {
        if (headers == null || sourceField == null) {
            return -1;
        }
        String normalized = PositionStandardField.normalize(sourceField);
        for (int i = 0; i < headers.size(); i++) {
            if (PositionStandardField.normalize(headers.get(i)).equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    /** Resolve user-confirmed semantics independently for each Sheet without assuming identical headers. */
    public List<ImportConfirmRequest.MappingItem> mappingsForSheet(ExcelRawSheet sheet, ImportConfirmRequest request) {
        Map<String, List<ImportConfirmRequest.MappingItem>> overrides = request.getSheetMappings();
        if (overrides != null && overrides.containsKey(sheet.getSheetName())) {
            return overrides.get(sheet.getSheetName());
        }
        List<ImportConfirmRequest.MappingItem> resolved = new ArrayList<>();
        for (ImportConfirmRequest.MappingItem mapping : request.getMappings()) {
            String target = mapping.getTargetField();
            int direct = findColumnIndex(sheet.getHeaders(), mapping.getSourceField());
            if (direct >= 0) {
                resolved.add(mapping);
                continue;
            }
            if (request.getSheetNames() == null || request.getSheetNames().isEmpty()
                    || sheet.getSheetName().equals(request.getSheetNames().get(0))) {
                resolved.add(mapping);
                continue;
            }
            if (target == null || target.trim().isEmpty()) continue;
            String alternative = null;
            for (String header : sheet.getHeaders()) {
                if (!target.equals(suggest(header).getSuggestedField())) continue;
                if (alternative != null) throw new BusinessException("Sheet “" + sheet.getSheetName() + "”的字段“" + target + "”有多个候选列，请分别确认映射");
                alternative = header;
            }
            if (alternative != null) {
                ImportConfirmRequest.MappingItem item = new ImportConfirmRequest.MappingItem();
                item.setSourceField(alternative);
                item.setTargetField(target);
                resolved.add(item);
            }
        }
        return resolved;
    }
}
