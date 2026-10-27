package com.gk.jobhelper.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobRemarkParserTest {
    private final JobRemarkParser parser = new JobRemarkParser();

    @Test
    void separatesServiceTermFromCertificateAndLocation() {
        List<JobRemarkParser.Condition> conditions = parser.parse(
                "须在本机关（单位）最低服务3年；须取得法律职业资格A证；工作地点在彭泽县。");
        assertTrue(conditions.stream().anyMatch(c -> "MIN_SERVICE_YEARS".equals(c.getType())
                && "POST_EMPLOYMENT".equals(c.getNature()) && Integer.valueOf(3).equals(c.getYears())));
        assertTrue(conditions.stream().anyMatch(c -> "CERTIFICATE".equals(c.getType())
                && "QUALIFICATION".equals(c.getNature())));
        assertTrue(conditions.stream().anyMatch(c -> "WORK_LOCATION".equals(c.getType())));
    }

    @Test
    void keepsUnknownQualificationTextTraceable() {
        List<JobRemarkParser.Condition> conditions = parser.parse("须通过特别能力测试");
        assertEquals("UNKNOWN", conditions.get(0).getType());
        assertEquals("须通过特别能力测试", conditions.get(0).getSourceText());
    }

    @Test
    void blankRemarkHasNoConditions() {
        assertTrue(parser.parse("  ").isEmpty());
    }

    @Test
    void keepsSeparatorsInsideBracketsInOneClause() {
        List<String> clauses = JobRemarkParser.splitClauses("须取得证书（A；B）；工作地点为南昌市");
        assertEquals(2, clauses.size());
        assertTrue(clauses.get(0).contains("A;B"));
    }

    @Test
    void exposesSourceOperatorAndParserVersion() {
        com.gk.jobhelper.entity.JobPosition position = new com.gk.jobhelper.entity.JobPosition();
        position.setAdditionalRequirements("法学专业须取得法律职业资格A证");
        JobRemarkParser.Condition condition = parser.parse(position).get(0);
        assertEquals("additionalRequirements", condition.getSourceField());
        assertEquals("IF_THEN", condition.getOperator());
        assertEquals(JobRemarkParser.PARSER_VERSION, condition.getParserVersion());
    }
}
