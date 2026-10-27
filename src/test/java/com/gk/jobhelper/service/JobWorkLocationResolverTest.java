package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.entity.JobPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobWorkLocationResolverTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JobWorkLocationResolver resolver;

    JobWorkLocationResolverTest() throws IOException {
        resolver = new JobWorkLocationResolver(new AdministrativeDivisionCatalog(mapper), mapper);
    }

    @Test
    void grassrootsAgencyOnlyProvidesInferredJurisdiction() {
        JobPosition position = new JobPosition();
        position.setDepartmentName("进贤县前坊镇人民政府");
        JobWorkLocationResolver.Result result = resolver.resolve(position);
        assertEquals("INFERRED", result.getStatus());
        assertEquals("ADMIN_JURISDICTION", result.getType());
        assertEquals("360124", result.getCode());
        assertEquals("江西省 南昌市 进贤县", result.getName());
    }

    @Test
    void provincialAgencyMustNotInheritProvincialCapital() {
        JobPosition position = new JobPosition();
        position.setDepartmentName("江西省野生动植物保护中心");
        assertEquals("UNKNOWN", resolver.resolve(position).getStatus());
        assertNull(resolver.resolve(position).getCode());
    }

    @Test
    void explicitSiteOverridesAgencyJurisdiction() {
        JobPosition position = new JobPosition();
        position.setDepartmentName("江西省自然保护地建设中心");
        position.setRemark("工作地点在彭泽县。");
        JobWorkLocationResolver.Result result = resolver.resolve(position);
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("360430", result.getCode());
        assertEquals("REMARK", result.getSource());
    }

    @Test
    void dispersedGrassrootsStationsRemainMultiSite() {
        JobPosition position = new JobPosition();
        position.setDepartmentName("江西鄱阳湖国家级自然保护区管理局");
        position.setRemark("工作地点在沿鄱阳湖11个县、乡基层保护站");
        assertEquals("MULTI_SITE", resolver.resolve(position).getStatus());
    }

    @Test
    void telephoneAreaCodeIsNotLocationEvidence() {
        JobPosition position = new JobPosition();
        position.setRawData("{\"联系电话\":\"0791-12345678\"}");
        assertEquals("UNKNOWN", resolver.resolve(position).getStatus());
    }
}
