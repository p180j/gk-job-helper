package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.RecruitmentPosition;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maps the shared normalized recruitment fields to the persisted position model. */
@Component
public class RecruitmentPositionFieldMapper {
    public RecruitmentPosition position(Map<String, String> data, Long noticeId, Long attachmentId,
                                        String sourceType, String source, int row, String raw) {
        RecruitmentPosition position = new RecruitmentPosition();
        position.setNoticeId(noticeId);
        position.setSourceAttachmentId(attachmentId);
        position.setSourceType(sourceType);
        position.setPositionName(value(data, "POSITION_NAME"));
        position.setPositionCode(value(data, "POSITION_CODE"));
        position.setOrganizationName(value(data, "ORGANIZATION"));
        position.setDepartmentName(value(data, "DEPARTMENT"));
        position.setRecruitCount(number(value(data, "RECRUIT_COUNT")));
        position.setWorkLocation(value(data, "WORK_LOCATION"));
        position.setEducationRequirement(value(data, "EDUCATION"));
        position.setDegreeRequirement(value(data, "DEGREE"));
        position.setMajorRequirement(value(data, "MAJOR"));
        position.setAgeRequirement(value(data, "AGE"));
        position.setWorkYearsRequirement(value(data, "WORK_EXPERIENCE"));
        position.setResponsibility(value(data, "RESPONSIBILITY"));
        position.setOtherRequirement(join(value(data, "REQUIREMENT"), value(data, "OTHER")));
        position.setPreferredRequirement(value(data, "PREFERRED"));
        position.setRawRequirement(raw);
        position.setSourceSheet(source);
        position.setSourceRow(row);
        position.setCreatedAt(LocalDateTime.now());
        position.setUpdatedAt(LocalDateTime.now());
        return position;
    }

    public String raw(Map<String, String> data) {
        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!trim(entry.getValue()).isEmpty()) {
                if (value.length() > 0) value.append("；");
                value.append(entry.getKey()).append("：").append(trim(entry.getValue()));
            }
        }
        return value.toString();
    }

    public boolean skip(String name, Map<String, String> data) {
        String value = trim(name);
        return value.isEmpty() || value.matches("^(合计|总计|备注|说明|注|注释|附件).*$")
                || raw(data).matches("^(合计|总计|备注|说明|注|注释).*");
    }

    public String trim(String value) { return value == null ? "" : value.trim(); }

    private String value(Map<String, String> data, String field) {
        String value = trim(data.get(field));
        return value.isEmpty() ? null : value;
    }
    private Integer number(String value) {
        try {
            String number = trim(value).replaceAll("[^0-9]", "");
            return number.isEmpty() ? null : Integer.valueOf(number);
        } catch (NumberFormatException ignored) { return null; }
    }
    private String join(String first, String second) {
        if (first == null) return second;
        return second == null ? first : first + "；" + second;
    }
}
