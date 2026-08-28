package com.gk.jobhelper.service;

import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.matcher.MatchItemResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 综合匹配结果聚合规则测试（固定规则，禁止评分/概率）:
 * 存在 NOT_MATCH -> NOT_MATCH；否则存在 UNCERTAIN -> UNCERTAIN；否则 MATCH
 */
class MatchAggregateRuleTest {

    @Test
    void allMatchShouldAggregateMatch() {
        List<MatchItemResult> items = Arrays.asList(
                item(ConditionType.EDUCATION, MatchResult.MATCH),
                item(ConditionType.AGE, MatchResult.MATCH),
                item(ConditionType.POLITICAL, MatchResult.MATCH),
                item(ConditionType.WORK_EXPERIENCE, MatchResult.MATCH));
        assertEquals(MatchResult.MATCH, JobMatchService.aggregate(items));
    }

    @Test
    void singleUncertainShouldAggregateUncertain() {
        List<MatchItemResult> items = Arrays.asList(
                item(ConditionType.EDUCATION, MatchResult.MATCH),
                item(ConditionType.AGE, MatchResult.MATCH),
                item(ConditionType.POLITICAL, MatchResult.UNCERTAIN),
                item(ConditionType.WORK_EXPERIENCE, MatchResult.MATCH));
        assertEquals(MatchResult.UNCERTAIN, JobMatchService.aggregate(items));
    }

    @Test
    void singleNotMatchShouldAggregateNotMatch() {
        List<MatchItemResult> items = Arrays.asList(
                item(ConditionType.EDUCATION, MatchResult.MATCH),
                item(ConditionType.AGE, MatchResult.NOT_MATCH),
                item(ConditionType.POLITICAL, MatchResult.MATCH),
                item(ConditionType.WORK_EXPERIENCE, MatchResult.MATCH));
        assertEquals(MatchResult.NOT_MATCH, JobMatchService.aggregate(items));
    }

    @Test
    void notMatchPlusUncertainShouldAggregateNotMatch() {
        // NOT_MATCH 优先级高于 UNCERTAIN
        List<MatchItemResult> items = Arrays.asList(
                item(ConditionType.EDUCATION, MatchResult.UNCERTAIN),
                item(ConditionType.AGE, MatchResult.NOT_MATCH),
                item(ConditionType.POLITICAL, MatchResult.UNCERTAIN),
                item(ConditionType.WORK_EXPERIENCE, MatchResult.MATCH));
        assertEquals(MatchResult.NOT_MATCH, JobMatchService.aggregate(items));
    }

    @Test
    void multipleNotMatchShouldAggregateNotMatch() {
        List<MatchItemResult> items = Arrays.asList(
                item(ConditionType.EDUCATION, MatchResult.NOT_MATCH),
                item(ConditionType.AGE, MatchResult.NOT_MATCH));
        assertEquals(MatchResult.NOT_MATCH, JobMatchService.aggregate(items));
    }

    @Test
    void emptyItemsShouldAggregateMatch() {
        assertEquals(MatchResult.MATCH, JobMatchService.aggregate(Collections.emptyList()));
    }

    private MatchItemResult item(ConditionType type, MatchResult result) {
        return new MatchItemResult(type, result, null, null, "测试");
    }
}
