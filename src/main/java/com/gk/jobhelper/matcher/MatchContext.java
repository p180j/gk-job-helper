package com.gk.jobhelper.matcher;

import com.gk.jobhelper.dto.CareerWorkVO;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 匹配执行上下文。
 * referenceDate：年龄等条件计算的基准日期；未指定时默认当前日期。
 */
public class MatchContext {

    private final LocalDate referenceDate;
    private final List<CareerWorkVO> workExperiences;

    private MatchContext(LocalDate referenceDate, List<CareerWorkVO> workExperiences) {
        this.referenceDate = referenceDate;
        this.workExperiences = workExperiences == null ? Collections.emptyList() : workExperiences;
    }

    /** referenceDate 为 null 时使用当前日期 */
    public static MatchContext of(LocalDate referenceDate) {
        return new MatchContext(referenceDate == null ? LocalDate.now() : referenceDate, Collections.emptyList());
    }

    public static MatchContext of(LocalDate referenceDate, List<CareerWorkVO> workExperiences) {
        return new MatchContext(referenceDate == null ? LocalDate.now() : referenceDate, workExperiences);
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }
    public List<CareerWorkVO> getWorkExperiences() { return workExperiences; }
}
