package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;

/**
 * 岗位资格条件匹配器统一接口。
 * 每个实现只负责一种条件类型，禁止把所有规则堆进单个超长方法。
 */
public interface JobConditionMatcher {

    /** 本匹配器负责的条件类型 */
    ConditionType support();

    /**
     * 执行匹配，返回该条件的判定结果（MATCH / UNCERTAIN / NOT_MATCH + 中文 reason）
     */
    MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context);
}
