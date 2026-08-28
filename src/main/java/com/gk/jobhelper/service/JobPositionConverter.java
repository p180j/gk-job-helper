package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.constant.PositionStandardField;
import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelRawRow;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.dto.ImportFailedItem;
import com.gk.jobhelper.entity.JobPosition;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 行转换器：按用户确认的映射将 Excel 原始行转换为标准 JobPosition
 * - raw_data 保存该行完整 JSON（含未映射/未识别列，不丢数据）
 * - recruit_count 等数字字段解析失败置 null，不导致行失败
 * - positionCode/positionName 映射存在但行值为空时，该行导入失败并记录原因
 */
@Component
public class JobPositionConverter {

    private static final Pattern DIGITS = Pattern.compile("\\d+");

    private final ObjectMapper objectMapper;
    private final FieldMappingService fieldMappingService;

    public JobPositionConverter(ObjectMapper objectMapper, FieldMappingService fieldMappingService) {
        this.objectMapper = objectMapper;
        this.fieldMappingService = fieldMappingService;
    }

    /**
     * 转换整个 Sheet
     *
     * @param mappings 已通过合法性校验的用户确认映射
     */
    public ConversionResult convert(ExcelRawSheet sheet, List<ImportConfirmRequest.MappingItem> mappings,
                                    Long importFileId) {
        // 标准字段 -> 取值列下标列表（同一目标可映射多个来源列，取第一个非空值）
        Map<String, List<Integer>> targetColumns = new LinkedHashMap<>();
        for (ImportConfirmRequest.MappingItem mapping : mappings) {
            String target = trimToNull(mapping.getTargetField());
            if (target == null) {
                continue; // 用户指定该列不导入
            }
            int column = fieldMappingService.findColumnIndex(sheet.getHeaders(), mapping.getSourceField());
            if (column < 0) {
                continue; // 防御：理论上 Service 已校验存在
            }
            targetColumns.computeIfAbsent(target, k -> new ArrayList<>()).add(column);
        }

        ConversionResult result = new ConversionResult();
        for (ExcelRawRow row : sheet.getRows()) {
            try {
                result.getPositions().add(convertRow(sheet, row, targetColumns, importFileId));
            } catch (RowRejectException e) {
                result.getFailures().add(new ImportFailedItem(row.getRowNumber(), e.getMessage()));
            } catch (Exception e) {
                result.getFailures().add(new ImportFailedItem(row.getRowNumber(), "行数据转换异常: " + e.getMessage()));
            }
        }
        return result;
    }

    private JobPosition convertRow(ExcelRawSheet sheet, ExcelRawRow row,
                                   Map<String, List<Integer>> targetColumns, Long importFileId) {
        JobPosition position = new JobPosition();
        position.setImportFileId(importFileId);
        position.setSourceSheet(sheet.getSheetName());
        position.setSourceRow(row.getRowNumber());

        position.setDepartmentName(firstNonBlank(row, targetColumns.get("departmentName")));
        position.setOrganizationName(firstNonBlank(row, targetColumns.get("organizationName")));
        position.setPositionName(firstNonBlank(row, targetColumns.get("positionName")));
        position.setPositionCode(firstNonBlank(row, targetColumns.get("positionCode")));
        position.setProvince(firstNonBlank(row, targetColumns.get("province")));
        position.setCity(firstNonBlank(row, targetColumns.get("city")));
        position.setDistrict(firstNonBlank(row, targetColumns.get("district")));
        position.setRecruitCount(parseRecruitCount(row, targetColumns.get("recruitCount")));
        position.setEducationRequirement(firstNonBlank(row, targetColumns.get("educationRequirement")));
        position.setDegreeRequirement(firstNonBlank(row, targetColumns.get("degreeRequirement")));
        position.setMajorRequirement(firstNonBlank(row, targetColumns.get("majorRequirement")));
        position.setMajorCodes(firstNonBlank(row, targetColumns.get("majorCodes")));
        position.setAgeRequirement(firstNonBlank(row, targetColumns.get("ageRequirement")));
        position.setPoliticalRequirement(firstNonBlank(row, targetColumns.get("politicalRequirement")));
        position.setWorkYearRequirement(firstNonBlank(row, targetColumns.get("workYearRequirement")));
        position.setFreshGraduateRequirement(firstNonBlank(row, targetColumns.get("freshGraduateRequirement")));
        position.setHouseholdRequirement(firstNonBlank(row, targetColumns.get("householdRequirement")));
        position.setServiceProjectRequirement(firstNonBlank(row, targetColumns.get("serviceProjectRequirement")));
        position.setCertificateRequirement(firstNonBlank(row, targetColumns.get("certificateRequirement")));
        position.setGenderRequirement(firstNonBlank(row, targetColumns.get("genderRequirement")));
        position.setPositionDescription(firstNonBlank(row, targetColumns.get("positionDescription")));
        position.setRemark(firstNonBlank(row, targetColumns.get("remark")));

        // 行级必填校验：映射了关键列但值为空 -> 该行失败
        if (targetColumns.containsKey("positionCode") && isBlank(position.getPositionCode())) {
            throw new RowRejectException("职位代码为空");
        }
        if (targetColumns.containsKey("positionName") && isBlank(position.getPositionName())) {
            throw new RowRejectException("职位名称为空");
        }

        position.setRawData(buildRawJson(sheet.getHeaders(), row));
        LocalDateTime now = LocalDateTime.now();
        position.setCreatedAt(now);
        position.setUpdatedAt(now);
        return position;
    }

    /**
     * 构建该行完整 JSON（表头 -> 原始值），未映射/未识别列同样保留
     */
    private String buildRawJson(List<String> headers, ExcelRawRow row) {
        Map<String, String> raw = new LinkedHashMap<>();
        Set<String> usedKeys = new HashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = rawKey(headers.get(i), i, usedKeys);
            String value = row.getCells().get(i);
            raw.put(key, value == null ? "" : value);
        }
        // 超出表头长度的列以"列N"保留
        for (Map.Entry<Integer, String> entry : row.getCells().entrySet()) {
            if (entry.getKey() >= headers.size()) {
                raw.put("列" + (entry.getKey() + 1), entry.getValue() == null ? "" : entry.getValue());
            }
        }
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            throw new RowRejectException("原始行 JSON 序列化失败: " + e.getMessage());
        }
    }

    private String rawKey(String header, int columnIndex, Set<String> usedKeys) {
        String key = header == null ? "" : header.trim();
        if (key.isEmpty()) {
            key = "列" + (columnIndex + 1);
        }
        String candidate = key;
        int suffix = 2;
        while (usedKeys.contains(candidate)) {
            candidate = key + "(" + suffix + ")";
            suffix++;
        }
        usedKeys.add(candidate);
        return candidate;
    }

    /**
     * 数字字段宽松解析: "5"->5, "5人"->5, "若干"->null（解析失败不抛异常）
     */
    private Integer parseRecruitCount(ExcelRawRow row, List<Integer> columns) {
        String value = firstNonBlank(row, columns);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // 尝试提取数字部分
        }
        Matcher matcher = DIGITS.matcher(value);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignored) {
                // 数字超长等异常
            }
        }
        return null;
    }

    private String firstNonBlank(ExcelRawRow row, List<Integer> columns) {
        if (columns == null) {
            return null;
        }
        for (Integer column : columns) {
            String value = row.getCells().get(column);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        PositionStandardField field = PositionStandardField.byFieldName(trimmed);
        return field == null ? null : field.getFieldName();
    }

    /** 行级拒绝异常 */
    private static class RowRejectException extends RuntimeException {
        RowRejectException(String message) {
            super(message);
        }
    }
}
