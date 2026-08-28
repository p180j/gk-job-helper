package com.gk.jobhelper.constant;

import java.util.Locale;

/**
 * 岗位标准字段定义与同义词表
 * 匹配规则（不允许 AI 猜测，仅字典匹配）:
 * 1. 表头归一化后精确命中标准名(含英文驼峰名) -> EXACT
 * 2. 命中同义词 -> ALIAS
 * 3. 其他 -> UNKNOWN
 */
public enum PositionStandardField {

    DEPARTMENT_NAME("departmentName",
            new String[]{"招录机关"},
            new String[]{"招聘单位", "主管部门", "单位名称", "部门名称", "招录单位", "招考单位"}),

    ORGANIZATION_NAME("organizationName",
            new String[]{"用人司局"},
            new String[]{"用人单位", "内设机构"}),

    POSITION_NAME("positionName",
            new String[]{"招考职位"},
            new String[]{"职位名称", "岗位名称"}),

    POSITION_CODE("positionCode",
            new String[]{"职位代码"},
            new String[]{"岗位代码"}),

    PROVINCE("province",
            new String[]{"省份"},
            new String[]{"省", "省级", "工作地点省份"}),

    CITY("city",
            new String[]{"城市"},
            new String[]{"市", "工作地点"}),

    DISTRICT("district",
            new String[]{"区县"},
            new String[]{"县区", "地区", "行政区划"}),

    RECRUIT_COUNT("recruitCount",
            new String[]{"招考人数"},
            new String[]{"招聘人数", "招录人数", "录用人数", "人数"}),

    EDUCATION_REQUIREMENT("educationRequirement",
            new String[]{"学历"},
            new String[]{"学历要求", "最低学历", "学历层次"}),

    DEGREE_REQUIREMENT("degreeRequirement",
            new String[]{"学位"},
            new String[]{"学位要求", "最低学位"}),

    MAJOR_REQUIREMENT("majorRequirement",
            new String[]{"专业"},
            new String[]{"专业要求", "所需专业", "招考专业"}),

    MAJOR_CODES("majorCodes",
            new String[]{"专业代码"},
            new String[]{"专业类别代码", "专业编码"}),

    AGE_REQUIREMENT("ageRequirement",
            new String[]{"年龄"},
            new String[]{"年龄要求", "年龄上限"}),

    POLITICAL_REQUIREMENT("politicalRequirement",
            new String[]{"政治面貌"},
            new String[]{"政治面貌要求"}),

    WORK_YEAR_REQUIREMENT("workYearRequirement",
            new String[]{"基层工作最低年限"},
            new String[]{"基层工作经历", "基层工作年限", "工作年限要求"}),

    FRESH_GRADUATE_REQUIREMENT("freshGraduateRequirement",
            new String[]{"应届生"},
            new String[]{"应届毕业生", "是否应届", "应届身份", "应届生要求"}),

    HOUSEHOLD_REQUIREMENT("householdRequirement",
            new String[]{"户籍"},
            new String[]{"户籍要求", "户口", "户口所在地", "户籍所在地"}),

    SERVICE_PROJECT_REQUIREMENT("serviceProjectRequirement",
            new String[]{"服务基层项目"},
            new String[]{"服务基层项目要求", "基层服务项目", "服务基层项目人员"}),

    CERTIFICATE_REQUIREMENT("certificateRequirement",
            new String[]{"证书"},
            new String[]{"证书要求", "资格证书", "职业资格证书"}),

    GENDER_REQUIREMENT("genderRequirement",
            new String[]{"性别"},
            new String[]{"性别要求"}),

    POSITION_DESCRIPTION("positionDescription",
            new String[]{"职位描述"},
            new String[]{"职位简介", "岗位职责", "职位介绍", "工作职责"}),

    REMARK("remark",
            new String[]{"备注"},
            new String[]{"其他条件", "其他要求", "备注信息"});

    private final String fieldName;
    private final String[] exactNames;
    private final String[] aliasNames;

    PositionStandardField(String fieldName, String[] exactNames, String[] aliasNames) {
        this.fieldName = fieldName;
        this.exactNames = exactNames;
        this.aliasNames = aliasNames;
    }

    public String getFieldName() {
        return fieldName;
    }

    /** 是否精确命中（归一化后的表头，含英文驼峰名比较） */
    public boolean matchesExact(String normalizedHeader) {
        if (normalizedHeader.equals(normalize(fieldName))) {
            return true;
        }
        for (String name : exactNames) {
            if (normalizedHeader.equals(normalize(name))) {
                return true;
            }
        }
        return false;
    }

    /** 是否同义词命中（归一化后的表头） */
    public boolean matchesAlias(String normalizedHeader) {
        for (String name : aliasNames) {
            if (normalizedHeader.equals(normalize(name))) {
                return true;
            }
        }
        return false;
    }

    /** 按标准字段英文名查找，找不到返回 null */
    public static PositionStandardField byFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        String normalized = normalize(fieldName);
        for (PositionStandardField field : values()) {
            if (normalize(field.fieldName).equals(normalized)) {
                return field;
            }
        }
        return null;
    }

    /** 表头/字段名归一化: 去除空白(含全角空格)并转小写 */
    public static String normalize(String header) {
        if (header == null) {
            return "";
        }
        return header.replaceAll("[\\s\\u3000]+", "").toLowerCase(Locale.ROOT);
    }
}
