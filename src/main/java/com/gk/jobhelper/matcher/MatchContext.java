package com.gk.jobhelper.matcher;

import java.time.LocalDate;

/**
 * 匹配执行上下文。
 * referenceDate：年龄等条件计算的基准日期；未指定时默认当前日期。
 */
public class MatchContext {

    private final LocalDate referenceDate;

    private MatchContext(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }

    /** referenceDate 为 null 时使用当前日期 */
    public static MatchContext of(LocalDate referenceDate) {
        return new MatchContext(referenceDate == null ? LocalDate.now() : referenceDate);
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }
}
