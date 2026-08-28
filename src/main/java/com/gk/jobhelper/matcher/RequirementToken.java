package com.gk.jobhelper.matcher;

import java.util.Collections;
import java.util.List;

/**
 * 岗位专业要求解析后的单个条件片段。
 * 例如 "080902 软件工程"、"计算机类"、"计算机类(不含数字媒体技术)"、
 *      "计算机类及相关专业"、"计算机相关专业" 各解析为一个 token。
 */
public class RequirementToken {

    /** 原始片段文本（用于 reason 展示，保留原值） */
    private final String raw;
    /** 前导专业/类代码（如 080902、0809、0812），可空 */
    private final String code;
    /** 代码后的名称部分（或无代码时的整段名称），可空 */
    private final String name;
    /** 纯"相关专业/相近专业"等非标准范围表述 */
    private final boolean relatedOnly;
    /** "X及相关专业"形式：X 部分按正常规则评估，未命中时按不确定处理 */
    private final boolean relatedSuffix;
    /** 括号内"不含/不包括"排除的专业名称列表 */
    private final List<String> excludedNames;
    /** 存在无法可靠解析的括号限定内容（非排除、非代码），保守判 UNCERTAIN */
    private final boolean opaque;

    public RequirementToken(String raw, String code, String name,
                            boolean relatedOnly, boolean relatedSuffix, List<String> excludedNames) {
        this(raw, code, name, relatedOnly, relatedSuffix, excludedNames, false);
    }

    public RequirementToken(String raw, String code, String name,
                            boolean relatedOnly, boolean relatedSuffix, List<String> excludedNames,
                            boolean opaque) {
        this.raw = raw;
        this.code = code;
        this.name = name;
        this.relatedOnly = relatedOnly;
        this.relatedSuffix = relatedSuffix;
        this.excludedNames = excludedNames == null
                ? Collections.<String>emptyList() : Collections.unmodifiableList(excludedNames);
        this.opaque = opaque;
    }

    public String getRaw() {
        return raw;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isRelatedOnly() {
        return relatedOnly;
    }

    public boolean isRelatedSuffix() {
        return relatedSuffix;
    }

    public List<String> getExcludedNames() {
        return excludedNames;
    }

    public boolean isOpaque() {
        return opaque;
    }
}
