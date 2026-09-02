package com.gk.jobhelper.matcher;

/**
 * 结构化匹配证据（Iteration 4 起）：
 * 专业匹配时记录使用的专业目录、命中节点及其父级信息，
 * 用于追溯判定依据；其他条件匹配器可暂不提供。
 * 序列化为 JSON 保存于 job_match_item.evidence（VARCHAR，MySQL 兼容）。
 */
public class MatchEvidence {

    /** 使用的专业目录编码，如 MOE_UNDERGRADUATE_2024 */
    private String catalogCode;
    /** 使用的专业目录名称，如 普通高等学校本科专业目录(2026年) */
    private String catalogName;
    /** 用户专业命中的目录节点代码，如 080902 */
    private String majorCode;
    /** 用户专业命中的目录节点名称，如 软件工程 */
    private String majorName;
    /** 命中的要求节点(专业类/学科)代码，如 0809 */
    private String parentCode;
    /** 命中的要求节点(专业类/学科)名称，如 计算机类 */
    private String parentName;

    public MatchEvidence() {
    }

    public MatchEvidence(String catalogCode, String catalogName,
                         String majorCode, String majorName,
                         String parentCode, String parentName) {
        this.catalogCode = catalogCode;
        this.catalogName = catalogName;
        this.majorCode = majorCode;
        this.majorName = majorName;
        this.parentCode = parentCode;
        this.parentName = parentName;
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

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
}
