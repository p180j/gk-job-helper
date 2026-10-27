package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecommendationCandidateRow;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JobRegionResolverTest {
    private final JobRegionResolver resolver = new JobRegionResolver();

    @Test
    void confirmedWorkSiteCanMeetRegionPreference() {
        RecommendationCandidateRow row = row("CONFIRMED", "江西省 南昌市 进贤县");
        assertTrue(resolver.matches(Collections.singletonList("江西省/南昌市"), row));
        assertFalse(resolver.matches(Collections.singletonList("江西省/九江市"), row));
        assertEquals("江西省 南昌市 进贤县", resolver.resolve(row));
    }

    @Test
    void administrativeJurisdictionDoesNotEarnWorkSitePreference() {
        RecommendationCandidateRow row = row("INFERRED", "江西省 南昌市 进贤县");
        assertFalse(resolver.matches(Collections.singletonList("江西省/南昌市"), row));
        assertTrue(resolver.resolve(row).contains("待核实"));
    }

    @Test
    void unknownAndMultiSiteRemainExplicit() {
        assertEquals("工作地点未知", resolver.resolve(row("UNKNOWN", null)));
        assertEquals("多个工作地点（具体地点待确定）", resolver.resolve(row("MULTI_SITE", "沿湖基层站")));
    }

    private RecommendationCandidateRow row(String status, String name) {
        RecommendationCandidateRow row = new RecommendationCandidateRow();
        row.setWorkLocationStatus(status);
        row.setWorkLocationName(name);
        return row;
    }
}
