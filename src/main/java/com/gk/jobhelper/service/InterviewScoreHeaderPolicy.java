package com.gk.jobhelper.service;

/** Centralized semantic rules for recognizing interview-score workbook columns. */
final class InterviewScoreHeaderPolicy {
    private InterviewScoreHeaderPolicy() {}

    static boolean isPositionCode(String header) {
        return containsAny(header, "职位代码", "岗位代码");
    }

    static boolean isScore(String header) {
        if (header == null || header.isEmpty()) return false;
        if ("成绩".equals(header) || "总成绩".equals(header)) return true;
        boolean scoreValue = header.contains("成绩") || header.contains("分数") || header.endsWith("分");
        boolean scoreContext = header.contains("笔试") || header.contains("进面") || header.contains("进入面试");
        return scoreContext && scoreValue;
    }

    static boolean isDepartment(String header) {
        return containsAny(header, "招录机关", "部门名称", "招录单位");
    }

    static boolean isPositionName(String header) {
        return containsAny(header, "职位名称", "岗位名称", "报考职位");
    }

    private static boolean containsAny(String value, String... candidates) {
        if (value == null) return false;
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }
}
