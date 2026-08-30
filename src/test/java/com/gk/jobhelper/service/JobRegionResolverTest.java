package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecommendationCandidateRow;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JobRegionResolverTest {
    private final JobRegionResolver resolver = new JobRegionResolver();

    @Test
    void resolvesJiangxiCityFromTelephoneWhenRegionColumnsAreEmpty() {
        RecommendationCandidateRow row = row("宁都县乡镇机关", "{\"联系电话\":\"0797-6938803\"}");
        assertEquals("江西省 赣州市", resolver.resolve(row));
        assertTrue(resolver.matches(Collections.singletonList("江西省/赣州市"), row));
        assertFalse(resolver.matches(Collections.singletonList("江西省/南昌市"), row));
    }

    @Test
    void resolvesMunicipalityDistrictFromDepartmentName() {
        RecommendationCandidateRow row = row("上海市浦东新区某局", null);
        assertEquals("上海市 浦东新区", resolver.resolve(row));
        assertTrue(resolver.matches(Collections.singletonList("上海市/浦东新区"), row));
    }

    @Test
    void keepsExplicitImportedRegionCompatibleWithCanonicalPreference() {
        RecommendationCandidateRow row = row("某单位", null);
        row.setProvince("江西"); row.setCity("南昌");
        assertTrue(resolver.matches(Collections.singletonList("江西省/南昌市"), row));
    }

    private RecommendationCandidateRow row(String department, String rawData) {
        RecommendationCandidateRow row = new RecommendationCandidateRow();
        row.setDepartmentName(department); row.setRawData(rawData);
        return row;
    }
}
